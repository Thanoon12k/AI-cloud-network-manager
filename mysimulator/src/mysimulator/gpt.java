package mysimulator;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;
import java.util.*;
import java.text.DecimalFormat;

public class gpt {
    private static final int NUM_DATACENTERS = 3;
    private static final int VMS_PER_DATACENTER = 5;
    private static final int NUM_CLOUDLETS = 15;  // 5 cloudlets per datacenter

    public static void main(String[] args) {
        Log.printLine("Starting CloudSim Scenario with One Broker...");

        try {
            // Step 1: Initialize CloudSim
            int numUsers = 1;  // One broker managing all datacenters
            Calendar calendar = Calendar.getInstance();
            boolean traceFlag = false;  // Trace events for logging
            CloudSim.init(numUsers, calendar, traceFlag);

            // Step 2: Create Datacenters and One Broker
            List<Datacenter> datacenters = new ArrayList<>();
            DatacenterBroker broker = new DatacenterBroker("Broker");

            // Create 3 datacenters
            for (int i = 0; i < NUM_DATACENTERS; i++) {
                Datacenter datacenter = createDatacenter("Datacenter_" + i);
                datacenters.add(datacenter);
            }

            // Step 3: Create VMs and Cloudlets and Submit to the Broker
            List<Vm> vmList = new ArrayList<>();
            List<Cloudlet> cloudletList = new ArrayList<>();
            
            // Create VMs and Cloudlets for all datacenters and submit to the broker
            for (int i = 0; i < NUM_DATACENTERS; i++) {
                List<Vm> vms = createVMs(i, VMS_PER_DATACENTER);  // 5 VMs per datacenter
                List<Cloudlet> cloudlets = createCloudlets(i, NUM_CLOUDLETS);  // 5 cloudlets per datacenter
                
                // Add VMs and Cloudlets to the global lists
                vmList.addAll(vms);
                cloudletList.addAll(cloudlets);
            }
            
            // Submit VMs and Cloudlets to the broker
            broker.submitGuestList(vmList);
            broker.submitCloudletList(cloudletList);

            // Step 4: Start the Simulation
            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            // Step 5: Collect Results and Metrics
            List<Cloudlet> completedCloudlets = broker.getCloudletReceivedList();
            printCloudletList(completedCloudlets);  // Print cloudlet results
            
            // Additional metrics collection (host and datacenter performance)
            collectHostMetrics(datacenters);
            collectDatacenterMetrics(datacenters);

            Log.printLine("CloudSim Scenario with One Broker finished!");

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    // Create a Datacenter with a Host and Virtual Machines (VMs)
    private static Datacenter createDatacenter(String name) {
        List<Host> hostList = new ArrayList<>();
        List<Pe> peList = new ArrayList<>();

        int mips = 1000; // CPU power
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // Single PE per host

        int ram = 8192; // RAM (in MB)
        long storage = 1000000; // Storage (in MB)
        int bw = 10000; // Bandwidth (in Mbps)

        Host host = new Host(0, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList, new VmSchedulerTimeShared(peList));
        hostList.add(host);

        String arch = "x86";  // System architecture
        String os = "Linux";  // Operating system
        String vmm = "Xen";   // Virtual Machine Monitor
        double timeZone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;
        LinkedList<Storage> storageList = new LinkedList<>(); // No SAN devices for now

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, timeZone, cost, costPerMem, costPerStorage, costPerBw);

        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    // Create Virtual Machines (VMs) for each broker
    private static List<Vm> createVMs(int brokerId, int numVms) {
        List<Vm> vmList = new ArrayList<>();
        int mips = 250;
        long size = 10000; // VM image size (in MB)
        int ram = 512; // RAM (in MB)
        long bw = 1000;
        int pesNumber = 1; // Number of CPUs
        String vmm = "Xen"; // VMM name

        for (int i = 0; i < numVms; i++) {
            Vm vm = new Vm(i, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
            vmList.add(vm);
        }
        return vmList;
    }

    // Create Cloudlets for each broker
    private static List<Cloudlet> createCloudlets(int brokerId, int numCloudlets) {
        List<Cloudlet> cloudletList = new ArrayList<>();
        long length = 40000; // Cloudlet length (in MI)
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        for (int i = 0; i < numCloudlets; i++) {
            Cloudlet cloudlet = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet.setUserId(brokerId);
            cloudletList.add(cloudlet);
        }
        return cloudletList;
    }

    // Print Cloudlet Results
    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent + "Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (Cloudlet value : list) {
            cloudlet = value;
            Log.print(indent + cloudlet.getCloudletId() + indent);

            if (cloudlet.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
                Log.print("SUCCESS");
                Log.printLine(indent + cloudlet.getResourceId() + indent + cloudlet.getGuestId() + indent + dft.format(cloudlet.getActualCPUTime()) + indent + dft.format(cloudlet.getExecStartTime()) + indent + dft.format(cloudlet.getExecFinishTime()));
            }
        }
    }

    // Collect and print Host Metrics (CPU, RAM, Storage usage, VM allocation)
    private static void collectHostMetrics(List<Datacenter> datacenters) {
        Log.printLine("\nHost Metrics:");
        for (Datacenter datacenter : datacenters) {
            List<Host> hostList = datacenter.getHostList();
            for (Host host : hostList) {
                int totalVMs = host.getVmList().size();
                double totalCpuUtilization = 0.0;
                double totalRamUtilization = 0.0;
                double totalStorageUsage = 0.0;
                
                // Calculate CPU, RAM, and Storage Utilization
                for (Vm vm : host.getVmList()) {
                    totalCpuUtilization += vm.getMips();  // Adjust as necessary based on VM specs
                    totalRamUtilization += vm.getRam() / (double) host.getRamProvisioner().getRam();
                    totalStorageUsage += vm.getSize();  // VM storage size
                }

                // Calculate average CPU utilization
                double avgCpuUtilization = totalCpuUtilization / host.getVmList().size();

                // Log resource utilization
                Log.printLine("Host ID: " + host.getId());
                Log.printLine("   Average CPU Utilization: " + avgCpuUtilization + "%");
                Log.printLine("   RAM Utilization: " + (totalRamUtilization / host.getVmList().size()) * 100 + "%");
                Log.printLine("   Storage Usage: " + totalStorageUsage + " MB");
                Log.printLine("   VMs Running: " + totalVMs);
            }
        }
    }

    // Collect and print Datacenter Metrics (Number of Hosts, CPU, RAM, Storage usage)
    private static void collectDatacenterMetrics(List<Datacenter> datacenters) {
        Log.printLine("\nDatacenter Metrics:");
        for (Datacenter datacenter : datacenters) {
            List<Host> hostList = datacenter.getHostList();
            int totalHosts = hostList.size();
            int totalCPU = 0, totalRAM = 0, totalStorage = 0;
            for (Host host : hostList) {
                totalCPU += host.getPeList().size();
                totalRAM += host.getRamProvisioner().getRam();
                totalStorage += host.getStorage();
            }
            Log.printLine("Datacenter: " + datacenter.getName());
            Log.printLine("   Total Hosts: " + totalHosts);
            Log.printLine("   Total CPUs: " + totalCPU);
            Log.printLine("   Total RAM: " + totalRAM + " MB");
            Log.printLine("   Total Storage: " + totalStorage + " MB");
        }
    }
}
