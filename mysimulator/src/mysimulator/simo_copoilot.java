package mysimulator;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.provisioners.*;
import org.cloudbus.cloudsim.core.HostEntity;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Extended CloudSimExample7: 3 datacenters, 5 VMs each (15 VMs), one broker.
 * Collects cloudlet, VM, host, datacenter metrics plus cost, utilization and energy estimates.
 *
 * NOTE: This file preserves your original structure but fixes compilation issues:
 * - Replaced HostEntity usage with Host
 * - Avoided calling Datacenter.getCharacteristics() directly by storing characteristics when creating datacenters
 * - Added safe access (try/catch) for methods that may not exist in some CloudSim versions
 */
public class simo_copoilot {
    public static DatacenterBroker broker;
    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmlist;
    private static List<Datacenter> datacenters;

    // Stored datacenter characteristics and cost parameters to avoid calling protected/hidden getters
    private static final Map<Integer, DatacenterCharacteristics> dcCharacteristics = new HashMap<>();
    private static final Map<Integer, Double> dcCostMap = new HashMap<>();
    private static final Map<Integer, Double> dcCostPerMemMap = new HashMap<>();
    private static final Map<Integer, Double> dcCostPerStorageMap = new HashMap<>();
    private static final Map<Integer, Double> dcCostPerBwMap = new HashMap<>();

    public static void main(String[] args) {
        Log.println("Starting CloudSimExample7FullMetrics...");

        try {
            int num_user = 1;   // one broker/user
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            // Create 3 datacenters
            datacenters = new ArrayList<>();
            datacenters.add(createDatacenter("Datacenter_0"));
            datacenters.add(createDatacenter("Datacenter_1"));
            datacenters.add(createDatacenter("Datacenter_2"));

            // Create single broker
            broker = new DatacenterBroker("Broker_0");
            int brokerId = broker.getId();

            // Create VMs: 5 per datacenter (id shifts to keep unique VM ids)
            vmlist = new ArrayList<>();
            vmlist.addAll(createVM(brokerId, 5, 0));     // for Datacenter_0
            vmlist.addAll(createVM(brokerId, 5, 100));   // for Datacenter_1
            vmlist.addAll(createVM(brokerId, 5, 200));   // for Datacenter_2
            broker.submitGuestList(vmlist);

            // Create Cloudlets: 30 total (10 per datacenter)
            cloudletList = new ArrayList<>();
            cloudletList.addAll(createCloudlet(brokerId, 100, 0));
            
            broker.submitCloudletList(cloudletList);

            // Start simulation
            CloudSim.startSimulation();

            // Collect finished cloudlets
            List<Cloudlet> finishedCloudlets = broker.getCloudletReceivedList();

            // Stop simulation
            CloudSim.stopSimulation();

            // Compute makespan (max finish time)
            double makespan = computeMakespan(finishedCloudlets);

            // Print detailed metrics
            printCloudletMetrics(finishedCloudlets);
            printVmMetrics(vmlist, finishedCloudlets);
            printHostMetrics(datacenters, makespan, finishedCloudlets);
            printDatacenterCostSummary(datacenters, finishedCloudlets);
            printSimulationSummary(finishedCloudlets, makespan);

            Log.println("CloudSimExample7FullMetrics finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.println("Simulation terminated due to error.");
        }
    }

    // -------------------------
    // Creation helpers
    // -------------------------
    private static List<Vm> createVM(int userId, int vms, int idShift) {
        List<Vm> list = new ArrayList<>();
        long size = 10000; // image size (MB)
        int ram = 512;     // vm memory (MB)
        int mips = 1000;
        long bw = 1000;
        int pesNumber = 1;
        String vmm = "Xen";

        for (int i = 0; i < vms; i++) {
            Vm vm = new Vm(idShift + i, userId, mips, pesNumber, ram, bw, size, vmm,
                    new CloudletSchedulerTimeShared());
            list.add(vm);
        }
        return list;
    }

    private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift) {
        List<Cloudlet> list = new ArrayList<>();
        long length = 40000;
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        for (int i = 0; i < cloudlets; i++) {
            Cloudlet cl = new Cloudlet(idShift + i, length, pesNumber, fileSize, outputSize,
                    utilizationModel, utilizationModel, utilizationModel);
            cl.setUserId(userId);
            list.add(cl);
        }
        return list;
    }

    private static Datacenter createDatacenter(String name) {
        List<Host> hostList = new ArrayList<>();

        // Create a quad-core host (4 PEs)
        List<Pe> peList = new ArrayList<>();
        int mips = 1000;
        for (int i = 0; i < 4; i++) {
            peList.add(new Pe(i, new PeProvisionerSimple(mips)));
        }

        int hostId = 0;
        int ram = 16384; // host memory (MB)
        long storage = 1000000;
        int bw = 10000;

        hostList.add(new Host(hostId, new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw), storage, peList,
                new VmSchedulerTimeShared(peList)));

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.1;
        double costPerBw = 0.1;
        LinkedList<Storage> storageList = new LinkedList<>();

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics,
                    new VmAllocationPolicySimple(hostList), storageList, 0);

            // store characteristics and cost parameters for later use (avoid calling protected getters)
            if (datacenter != null) {
                int dcId = datacenter.getId();
                dcCharacteristics.put(dcId, characteristics);
                dcCostMap.put(dcId, cost);
                dcCostPerMemMap.put(dcId, costPerMem);
                dcCostPerStorageMap.put(dcId, costPerStorage);
                dcCostPerBwMap.put(dcId, costPerBw);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return datacenter;
    }

    // -------------------------
    // Metrics & printing
    // -------------------------
    private static void printCloudletMetrics(List<Cloudlet> list) {
        String indent = "    ";
        Log.println("\n========== CLOUDLET METRICS ==========");
        Log.println("ID" + indent + "Status" + indent + "DC" + indent + "VM" +
                indent + "CPU Time" + indent + "Start" + indent + "Finish");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (Cloudlet cl : list) {
            String status = cl.getStatus() == Cloudlet.CloudletStatus.SUCCESS ? "SUCCESS" : "FAILED";
            Log.print(cl.getCloudletId() + indent + status + indent);
            Log.println(cl.getResourceId() + indent + cl.getGuestId() + indent +
                    dft.format(cl.getActualCPUTime()) + indent +
                    dft.format(cl.getExecStartTime()) + indent +
                    dft.format(cl.getExecFinishTime()));
        }
    }

    private static void printVmMetrics(List<Vm> vms, List<Cloudlet> finishedCloudlets) {
        Log.println("\n========== VM METRICS ==========");
        DecimalFormat dft = new DecimalFormat("###.##");

        // Map VM id -> total CPU time executed (sum of cloudlets assigned)
        Map<Integer, Double> vmCpuTime = new HashMap<>();
        for (Cloudlet cl : finishedCloudlets) {
            int vmId = cl.getGuestId();
            vmCpuTime.put(vmId, vmCpuTime.getOrDefault(vmId, 0.0) + cl.getActualCPUTime());
        }

        for (Vm vm : vms) {
            double totalCpu = vmCpuTime.getOrDefault(vm.getId(), 0.0);
            // Attempt to get instantaneous utilization at simulation end (if API available)
            double util = Double.NaN;
            try {
                util = vm.getTotalUtilizationOfCpu(CloudSim.clock());
            } catch (Throwable t) {
                // fallback: N/A
            }
            Log.println("VM " + vm.getId() +
                    " | User: " + vm.getUserId() +
                    " | MIPS: " + vm.getMips() +
                    " | RAM: " + vm.getRam() +
                    " | BW: " + vm.getBw() +
                    " | Size: " + vm.getSize() +
                    " | TotalCloudletCPUTime: " + dft.format(totalCpu) +
                    " | InstantUtil(end): " + (Double.isNaN(util) ? "N/A" : dft.format(util)));
        }
    }

    private static void printHostMetrics(List<Datacenter> datacenters, double makespan, List<Cloudlet> finishedCloudlets) {
        Log.println("\n========== HOST METRICS & ENERGY ESTIMATES ==========");
        DecimalFormat dft = new DecimalFormat("###.##");

        // Simple energy model constants (Watts) — tune as needed
        double idlePower = 100.0; // Watts when idle
        double maxPower = 250.0;  // Watts at 100% CPU

        // Build a map VM id -> total CPU time (sum of cloudlets executed on that VM)
        Map<Integer, Double> vmCpuTime = new HashMap<>();
        for (Cloudlet cl : finishedCloudlets) {
            int vmId = cl.getGuestId();
            vmCpuTime.put(vmId, vmCpuTime.getOrDefault(vmId, 0.0) + cl.getActualCPUTime());
        }

        for (Datacenter dc : datacenters) {
            Log.println("Datacenter: " + dc.getName());

            // Iterate defensively over whatever objects are returned by getHostList()
            for (Object hostObj : dc.getHostList()) {
                Host host;
                if (hostObj instanceof Host) {
                    host = (Host) hostObj;
                } else {
                    // Not a Host instance (wrapper or different type) — skip safely
                    continue;
                }

                // Sum CPU time of all VMs placed on this host
                double hostTotalCpuTime = 0.0;
                int pes = 1;
                try { pes = host.getNumberOfPes(); } catch (Throwable ignored) { pes = 1; }

                try {
                    List<Vm> hostVms = host.getVmList();
                    if (hostVms != null) {
                        for (Vm vm : hostVms) {
                            hostTotalCpuTime += vmCpuTime.getOrDefault(vm.getId(), 0.0);
                        }
                    }
                } catch (Throwable ignored) {
                    // if host.getVmList() not available, hostTotalCpuTime remains 0
                }

                // Compute average host utilization fraction over the run:
                // utilization = total CPU seconds executed on host / (makespan * numberOfPes)
                double hostUtil = Double.NaN;
                if (makespan > 0 && pes > 0) {
                    hostUtil = hostTotalCpuTime / (makespan * pes);
                    // clamp to [0,1]
                    if (hostUtil < 0.0) hostUtil = 0.0;
                    if (hostUtil > 1.0) hostUtil = 1.0;
                }

                // Estimate power and energy using linear model
                double power = Double.isNaN(hostUtil) ? Double.NaN : idlePower + (maxPower - idlePower) * hostUtil;
                double energyJoules = Double.isNaN(power) ? Double.NaN : power * makespan; // W * s = J
                double energyKWh = Double.isNaN(energyJoules) ? Double.NaN : energyJoules / 3.6e6; // J -> kWh

                // Safe access to host properties
                long storage = -1;
                int availRam = -1;
                long hostBw = -1;
                double totalMips = Double.NaN;
                try { storage = host.getStorage(); } catch (Throwable ignored) { }
                try { availRam = host.getRamProvisioner().getAvailableRam(); } catch (Throwable ignored) { }
                try { hostBw = host.getBwProvisioner().getBw(); } catch (Throwable ignored) { }
                try { totalMips = host.getTotalMips(); } catch (Throwable ignored) { }

                Log.println(" Host " + host.getId() +
                        " | TotalMIPS: " + (Double.isNaN(totalMips) ? "N/A" : dft.format(totalMips)) +
                        " | AvailableRAM: " + (availRam < 0 ? "N/A" : availRam) +
                        " | Storage: " + (storage < 0 ? "N/A" : storage) +
                        " | BW: " + (hostBw < 0 ? "N/A" : hostBw) +
                        " | CPU Util(avg): " + (Double.isNaN(hostUtil) ? "N/A" : dft.format(hostUtil)) +
                        " | EstPower(W): " + (Double.isNaN(power) ? "N/A" : dft.format(power)) +
                        " | EstEnergy(kWh): " + (Double.isNaN(energyKWh) ? "N/A" : dft.format(energyKWh)));
            }
        }
    }


    private static void printDatacenterCostSummary(List<Datacenter> datacenters, List<Cloudlet> finishedCloudlets) {
        Log.println("\n========== DATACENTER COST SUMMARY ==========");
        DecimalFormat dft = new DecimalFormat("###.##");

        // Build map datacenterId -> Datacenter
        Map<Integer, Datacenter> dcById = new HashMap<>();
        for (Datacenter dc : datacenters) {
            dcById.put(dc.getId(), dc);
        }

        // For each cloudlet, compute cost using stored datacenter characteristics/cost params:
        // cost = costPerSec * cpuTime + costPerMem * vmRam * cpuTime + costPerStorage * vmSize * cpuTime + costPerBw * vmBw * cpuTime
        Map<Integer, Double> costPerDatacenter = new HashMap<>();
        double totalCostAll = 0.0;

        // Build VM lookup by id
        Map<Integer, Vm> vmById = new HashMap<>();
        for (Vm vm : vmlist) vmById.put(vm.getId(), vm);

        for (Cloudlet cl : finishedCloudlets) {
            int dcId = cl.getResourceId();
            Datacenter dc = dcById.get(dcId);
            if (dc == null) continue;

            // Use stored cost parameters (safer than calling getters that may not exist)
            double costPerSec = dcCostMap.getOrDefault(dcId, 0.0);
            double costPerMem = dcCostPerMemMap.getOrDefault(dcId, 0.0);
            double costPerStorage = dcCostPerStorageMap.getOrDefault(dcId, 0.0);
            double costPerBw = dcCostPerBwMap.getOrDefault(dcId, 0.0);

            double cpuCost = costPerSec * cl.getActualCPUTime(); // cost per second * seconds
            Vm vm = vmById.get(cl.getGuestId());
            double memCost = 0.0, storageCost = 0.0, bwCost = 0.0;
            if (vm != null) {
                memCost = costPerMem * vm.getRam() * cl.getActualCPUTime();
                storageCost = costPerStorage * vm.getSize() * cl.getActualCPUTime();
                bwCost = costPerBw * vm.getBw() * cl.getActualCPUTime();
            }
            double clCost = cpuCost + memCost + storageCost + bwCost;
            costPerDatacenter.put(dcId, costPerDatacenter.getOrDefault(dcId, 0.0) + clCost);
            totalCostAll += clCost;
        }

        for (Datacenter dc : datacenters) {
            double c = costPerDatacenter.getOrDefault(dc.getId(), 0.0);
            Log.println("Datacenter " + dc.getName() + " (id=" + dc.getId() + ") total estimated cost: " + dft.format(c));
        }
        Log.println("Total estimated cost across all datacenters: " + dft.format(totalCostAll));
    }

    private static void printSimulationSummary(List<Cloudlet> finishedCloudlets, double makespan) {
        DecimalFormat dft = new DecimalFormat("###.##");
        double totalCpuTime = 0.0;
        for (Cloudlet cl : finishedCloudlets) totalCpuTime += cl.getActualCPUTime();

        double avgCpuTime = finishedCloudlets.isEmpty() ? 0.0 : totalCpuTime / finishedCloudlets.size();

        Log.println("\n========== SIMULATION SUMMARY ==========");
        Log.println("Total cloudlets finished: " + finishedCloudlets.size());
        Log.println("Total CPU time (sum of cloudlets): " + dft.format(totalCpuTime) + " sec");
        Log.println("Average cloudlet CPU time: " + dft.format(avgCpuTime) + " sec");
        Log.println("Makespan (max finish time): " + dft.format(makespan) + " sec");
    }

    // -------------------------
    // Utility
    // -------------------------
    private static double computeMakespan(List<Cloudlet> finishedCloudlets) {
        double max = 0.0;
        for (Cloudlet cl : finishedCloudlets) {
            double finish = cl.getExecFinishTime();
            if (finish > max) max = finish;
        }
        return max;
    }
}
