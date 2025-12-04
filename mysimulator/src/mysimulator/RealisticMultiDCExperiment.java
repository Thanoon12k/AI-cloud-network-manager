package mysimulator;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;

/**
 * Fixed CloudSim 7 Compatible Multi-DC Simulator
 * - Corrected API method names (submitGuestList instead of submitVmList)
 * - Fixed Host type casting issues
 * - 3 DCs, 3 Brokers, 15 VMs (5 per broker), 100 cloudlets
 */
public class RealisticMultiDCExperiment {

    private static final int NUM_USERS = 1;
    private static final boolean TRACE_FLAG = false;

    // VM configuration
    private static final int VM_MIPS = 1000;
    private static final int VM_RAM_MB = 512;
    private static final long VM_SIZE_MB = 10000;
    private static final long VM_BW_MBIT = 1000;

    // Host configuration
    private static final int HOST_RAM_MB = 4096;
    private static final long HOST_STORAGE_MB = 100000;
    private static final long HOST_BW_MBIT = 10000;
    private static final int HOST_PE_LARGE = 2;
    private static final int HOST_PE_SMALL = 1;
    private static final int HOST_PE_MIPS = 2000;

    // Cost configuration
    private static final double COST_PER_SECOND_CPU = 0.002;
    private static final double COST_PER_MB_RAM = 0.001;
    private static final double COST_PER_MB_STORAGE = 0.00001;
    private static final double COST_PER_MB_BW = 0.00005;

    // Global collections
    private static final List<Datacenter> datacenters = new ArrayList<>();
    private static final List<DatacenterBroker> brokers = new ArrayList<>();
    private static final List<Vm> vmList = new ArrayList<>();
    private static final List<Cloudlet> allCloudlets = new ArrayList<>();

    private static final DecimalFormat DF = new DecimalFormat("###.###");

    public static void main(String[] args) {
        Log.printLine("════════════════════════════════════════════════════════════════════");
        Log.printLine("  Realistic Multi-DC CloudSim Experiment (3 DCs, 15 VMs, 100 tasks) ");
        Log.printLine("════════════════════════════════════════════════════════════════════");

        try {
            CloudSim.init(NUM_USERS, Calendar.getInstance(), TRACE_FLAG);

            createDatacenters(3);
            createBrokers(3);
            createAndAssignVMs(5);        // 5 VMs per broker
            createAndAssignCloudlets(100); // 100 cloudlets total

            Log.printLine("\n→ Starting CloudSim simulation...");
            double start = CloudSim.clock();
            CloudSim.startSimulation();
            CloudSim.stopSimulation();
            double end = CloudSim.clock();

            Log.printLine("→ Simulation finished at time: " + DF.format(end) + " seconds");

            List<Cloudlet> finished = collectFinishedCloudlets();
            exportCloudletMetrics(finished);
            exportVmMetrics();
            exportHostMetrics();
            exportDatacenterSummary();
            exportPerformanceMetrics(finished);
            exportCostMetrics(finished);

            printSummaryToConsole(finished, end - start);

            Log.printLine("\n✓ Simulation and exports completed.");
        } catch (Exception e) {
            Log.printLine("✗ Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /* INFRASTRUCTURE CREATION */

    private static void createDatacenters(int count) throws Exception {
        for (int i = 0; i < count; i++) {
            datacenters.add(createSingleDatacenter("Datacenter_" + i));
        }
        Log.printLine("→ Created " + datacenters.size() + " datacenters.");
    }

    private static Datacenter createSingleDatacenter(String name) throws Exception {
        List<Host> hostList = new ArrayList<>();

        // Host 0: 2 PEs
        List<Pe> peList1 = new ArrayList<>();
        for (int i = 0; i < HOST_PE_LARGE; i++) {
            peList1.add(new Pe(i, new PeProvisionerSimple(HOST_PE_MIPS)));
        }

        // Host 1: 1 PE
        List<Pe> peList2 = new ArrayList<>();
        for (int i = 0; i < HOST_PE_SMALL; i++) {
            peList2.add(new Pe(i, new PeProvisionerSimple(HOST_PE_MIPS)));
        }

        hostList.add(
            new Host(
                0,
                new RamProvisionerSimple(HOST_RAM_MB),
                new BwProvisionerSimple(HOST_BW_MBIT),
                HOST_STORAGE_MB,
                peList1,
                new VmSchedulerTimeShared(peList1)
            )
        );

        hostList.add(
            new Host(
                1,
                new RamProvisionerSimple(HOST_RAM_MB),
                new BwProvisionerSimple(HOST_BW_MBIT),
                HOST_STORAGE_MB,
                peList2,
                new VmSchedulerTimeShared(peList2)
            )
        );

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
            "x86", "Linux", "Xen",
            hostList,
            0.0,
            COST_PER_SECOND_CPU,
            COST_PER_MB_RAM,
            COST_PER_MB_STORAGE,
            COST_PER_MB_BW
        );

        return new Datacenter(
            name,
            characteristics,
            new VmAllocationPolicySimple(hostList),
            new LinkedList<Storage>(),
            0
        );
    }

    private static void createBrokers(int count) throws Exception {
        for (int i = 0; i < count; i++) {
            DatacenterBroker broker = new DatacenterBroker("Broker_" + i);
            brokers.add(broker);
        }
        Log.printLine("→ Created " + brokers.size() + " brokers.");
    }

    /* VM CREATION & ASSIGNMENT - FIXED */

    private static void createAndAssignVMs(int vmsPerBroker) {
        int vmId = 0;
        for (DatacenterBroker broker : brokers) {
            List<Vm> brokerVMs = new ArrayList<>();
            for (int i = 0; i < vmsPerBroker; i++) {
                Vm vm = new Vm(
                    vmId,
                    broker.getId(),
                    VM_MIPS,
                    1,
                    VM_RAM_MB,
                    VM_BW_MBIT,
                    VM_SIZE_MB,
                    "Xen",
                    new CloudletSchedulerTimeShared()
                );
                brokerVMs.add(vm);
                vmList.add(vm);
                vmId++;
            }
            // ✓ FIXED: Use submitGuestList instead of submitVmList
            broker.submitGuestList(brokerVMs);
        }
        Log.printLine("→ Created " + vmList.size() + " VMs (5 per broker).");
    }

    /* CLOUDLET CREATION & MAPPING */

    private static void createAndAssignCloudlets(int totalCloudlets) {
        int brokersCount = brokers.size();
        int basePerBroker = totalCloudlets / brokersCount;
        int remainder = totalCloudlets % brokersCount;

        int cloudletId = 0;
        int vmsPerBroker = vmList.size() / brokersCount;

        UtilizationModel cpuUtil = new UtilizationModelFull();
        UtilizationModel ramUtil = new UtilizationModelFull();
        UtilizationModel bwUtil = new UtilizationModelFull();

        Random random = new Random(42);

        for (int brokerIdx = 0; brokerIdx < brokersCount; brokerIdx++) {
            DatacenterBroker broker = brokers.get(brokerIdx);

            int brokerCloudletsCount = basePerBroker + (brokerIdx < remainder ? 1 : 0);
            List<Cloudlet> brokerCloudlets = new ArrayList<>();

            for (int i = 0; i < brokerCloudletsCount; i++) {
                long length = 200000 + random.nextInt(200001); // 200k–400k MI
                long fileSize = 300;
                long outputSize = 300;

                Cloudlet cl = new Cloudlet(
                    cloudletId,
                    length,
                    1,
                    fileSize,
                    outputSize,
                    cpuUtil,
                    ramUtil,
                    bwUtil
                );

                cl.setUserId(broker.getId());

                // Map to one of this broker's VMs
                int vmLocalIndex = i % vmsPerBroker;
                int vmGlobalId = brokerIdx * vmsPerBroker + vmLocalIndex;
                cl.setVmId(vmGlobalId);

                brokerCloudlets.add(cl);
                allCloudlets.add(cl);
                cloudletId++;
            }

            broker.submitCloudletList(brokerCloudlets);
            Log.printLine("→ Broker_" + brokerIdx + " received " + brokerCloudlets.size()
                + " cloudlets mapped to VMs " + (brokerIdx * vmsPerBroker)
                + "-" + (brokerIdx * vmsPerBroker + vmsPerBroker - 1));
        }

        Log.printLine("→ Total cloudlets created: " + allCloudlets.size());
    }

    /* METRIC COLLECTION */

    private static List<Cloudlet> collectFinishedCloudlets() {
        List<Cloudlet> finished = new ArrayList<>();
        for (DatacenterBroker broker : brokers) {
            finished.addAll(broker.getCloudletReceivedList());
        }
        Log.printLine("\n→ Finished cloudlets: " + finished.size());
        return finished;
    }

    /* CSV EXPORTS - FIXED HOST CASTING */

    private static void exportCloudletMetrics(List<Cloudlet> cloudlets) {
        try (FileWriter writer = new FileWriter("cloudlet_metrics.csv")) {
            writer.write("Cloudlet_ID,VM_ID,Status,Datacenter_ID,Length_MI,Exec_Start,Exec_Finish,Actual_CPU_Time,Waiting_Time\n");
            for (Cloudlet cl : cloudlets) {
                double wait = cl.getExecStartTime() - cl.getSubmissionTime();
                writer.write(
                    cl.getCloudletId() + "," +
                    cl.getVmId() + "," +
                    cl.getStatus() + "," +
                    cl.getResourceId() + "," +
                    cl.getCloudletLength() + "," +
                    DF.format(cl.getExecStartTime()) + "," +
                    DF.format(cl.getExecFinishTime()) + "," +
                    DF.format(cl.getActualCPUTime()) + "," +
                    DF.format(wait) + "\n"
                );
            }
            Log.printLine("✓ Exported cloudlet_metrics.csv");
        } catch (IOException e) {
            Log.printLine("✗ Error writing cloudlet_metrics.csv: " + e.getMessage());
        }
    }

    private static void exportVmMetrics() {
        try (FileWriter writer = new FileWriter("vm_metrics.csv")) {
            writer.write("VM_ID,MIPS,RAM_MB,BW_Mbps,PEs,Host_ID,Status,Datacenter\n");
            for (Vm vm : vmList) {
                // ✓ FIXED: Handle Host type properly
                Object hostObj = vm.getHost();
                String hostId = "N/A";
                String dcName = "N/A";
                
                if (hostObj != null) {
                    // Cast to Host (or use reflection if HostEntity)
                    Host host = (Host) hostObj;
                    hostId = String.valueOf(host.getId());
                    
                    for (Datacenter dc : datacenters) {
                        if (dc.getHostList().contains(host)) {
                            dcName = dc.getName();
                            break;
                        }
                    }
                }
                
                String status = vm.isInMigration() ? "MIGRATING" : "ACTIVE";

                writer.write(
                    vm.getId() + "," +
                    vm.getMips() + "," +
                    vm.getRam() + "," +
                    vm.getBw() + "," +
                    vm.getNumberOfPes() + "," +
                    hostId + "," +
                    status + "," +
                    dcName + "\n"
                );
            }
            Log.printLine("✓ Exported vm_metrics.csv");
        } catch (IOException e) {
            Log.printLine("✗ Error writing vm_metrics.csv: " + e.getMessage());
        }
    }

    private static void exportHostMetrics() {
        try (FileWriter writer = new FileWriter("host_metrics.csv")) {
            writer.write("Host_ID,Datacenter,PEs,RAM_MB,BW_Mbps,Storage_MB,VMs_Allocated,Total_MIPS_Capacity\n");
            for (Datacenter dc : datacenters) {
                // ✓ FIXED: Proper generic handling
                @SuppressWarnings("unchecked")
                List<Host> hostList =  dc.getHostList();
                
                for (Host host : hostList) {
                    int totalMips = host.getPeList().size() * HOST_PE_MIPS;
                    writer.write(
                        host.getId() + "," +
                        dc.getName() + "," +
                        host.getPeList().size() + "," +
                        host.getRamProvisioner().getRam() + "," +
                        host.getBwProvisioner().getBw() + "," +
                        host.getStorage() + "," +
                        host.getVmList().size() + "," +
                        totalMips + "\n"
                    );
                }
            }
            Log.printLine("✓ Exported host_metrics.csv");
        } catch (IOException e) {
            Log.printLine("✗ Error writing host_metrics.csv: " + e.getMessage());
        }
    }

    private static void exportDatacenterSummary() {
        try (FileWriter writer = new FileWriter("datacenter_summary.csv")) {
            writer.write("Datacenter,Total_Hosts,Total_PEs,Total_VMs,Total_RAM_MB,Total_BW_Mbps,Total_Storage_MB\n");
            for (Datacenter dc : datacenters) {
                // ✓ FIXED: Proper generic handling
                @SuppressWarnings("unchecked")
                List<Host> hostList =  dc.getHostList();
                
                int totalHosts = hostList.size();
                int totalPes = 0;
                int totalVms = 0;
                int totalRam = 0;
                long totalBw = 0;
                long totalStorage = 0;

                for (Host host : hostList) {
                    totalPes += host.getPeList().size();
                    totalVms += host.getVmList().size();
                    totalRam += host.getRamProvisioner().getRam();
                    totalBw += host.getBwProvisioner().getBw();
                    totalStorage += host.getStorage();
                }

                writer.write(
                    dc.getName() + "," +
                    totalHosts + "," +
                    totalPes + "," +
                    totalVms + "," +
                    totalRam + "," +
                    totalBw + "," +
                    totalStorage + "\n"
                );
            }
            Log.printLine("✓ Exported datacenter_summary.csv");
        } catch (IOException e) {
            Log.printLine("✗ Error writing datacenter_summary.csv: " + e.getMessage());
        }
    }

    private static void exportPerformanceMetrics(List<Cloudlet> cloudlets) {
        try (FileWriter writer = new FileWriter("performance_metrics.csv")) {
            writer.write("Metric,Value\n");

            if (cloudlets.isEmpty()) {
                writer.write("Total_Cloudlets,0\n");
                writer.write("Successful_Cloudlets,0\n");
                writer.write("Failed_Cloudlets,0\n");
                return;
            }

            int success = 0;
            int failed = 0;
            double totalExec = 0;
            double totalWait = 0;
            double minExec = Double.MAX_VALUE;
            double maxExec = 0;

            for (Cloudlet cl : cloudlets) {
                if (cl.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
                    success++;
                } else {
                    failed++;
                }
                double exec = cl.getActualCPUTime();
                double wait = cl.getExecStartTime() - cl.getSubmissionTime();

                totalExec += exec;
                totalWait += wait;
                minExec = Math.min(minExec, exec);
                maxExec = Math.max(maxExec, exec);
            }

            double avgExec = totalExec / cloudlets.size();
            double avgWait = totalWait / cloudlets.size();
            double successRate = (double) success / cloudlets.size() * 100.0;

            writer.write("Total_Cloudlets," + cloudlets.size() + "\n");
            writer.write("Successful_Cloudlets," + success + "\n");
            writer.write("Failed_Cloudlets," + failed + "\n");
            writer.write("Success_Rate_Percent," + DF.format(successRate) + "\n");
            writer.write("Avg_Execution_Time_Sec," + DF.format(avgExec) + "\n");
            writer.write("Avg_Waiting_Time_Sec," + DF.format(avgWait) + "\n");
            writer.write("Min_Execution_Time_Sec," + DF.format(minExec) + "\n");
            writer.write("Max_Execution_Time_Sec," + DF.format(maxExec) + "\n");
            writer.write("Total_Execution_Time_Sec," + DF.format(totalExec) + "\n");
            writer.write("Total_VMs_Used," + vmList.size() + "\n");

            Log.printLine("✓ Exported performance_metrics.csv");
        } catch (IOException e) {
            Log.printLine("✗ Error writing performance_metrics.csv: " + e.getMessage());
        }
    }

    private static void exportCostMetrics(List<Cloudlet> cloudlets) {
        try (FileWriter writer = new FileWriter("cost_metrics.csv")) {
            writer.write("Cost_Type,Value_USD\n");

            double totalCompute = 0;
            double totalMemory = 0;
            double totalStorage = 0;
            double totalBw = 0;

            for (Datacenter dc : datacenters) {
                // ✓ FIXED: Proper generic handling
                @SuppressWarnings("unchecked")
                List<Host> hostList =  dc.getHostList();
                
                for (Host host : hostList) {
                    int pes = host.getPeList().size();
                    int ram = host.getRamProvisioner().getRam();
                    long storage = host.getStorage();
                    long bw = host.getBwProvisioner().getBw();

                    double hours = 1.0;
                    totalCompute += COST_PER_SECOND_CPU * pes * HOST_PE_MIPS * hours * 3600;
                    totalMemory += COST_PER_MB_RAM * ram * hours;
                    totalStorage += COST_PER_MB_STORAGE * storage * hours;
                    totalBw += COST_PER_MB_BW * (bw / 8.0) * hours;
                }
            }

            double totalCost = totalCompute + totalMemory + totalStorage + totalBw;

            writer.write("Compute_Cost," + DF.format(totalCompute) + "\n");
            writer.write("Memory_Cost," + DF.format(totalMemory) + "\n");
            writer.write("Storage_Cost," + DF.format(totalStorage) + "\n");
            writer.write("Bandwidth_Cost," + DF.format(totalBw) + "\n");
            writer.write("Total_Cost," + DF.format(totalCost) + "\n");

            if (!cloudlets.isEmpty()) {
                double costPerCloudlet = totalCost / cloudlets.size();
                writer.write("Cost_Per_Cloudlet," + DF.format(costPerCloudlet) + "\n");
            }

            Log.printLine("✓ Exported cost_metrics.csv");
        } catch (IOException e) {
            Log.printLine("✗ Error writing cost_metrics.csv: " + e.getMessage());
        }
    }

    /* CONSOLE SUMMARY */

    private static void printSummaryToConsole(List<Cloudlet> cloudlets, double simWallTime) {
        if (cloudlets.isEmpty()) {
            Log.printLine("\nNo finished cloudlets. Nothing to summarize.");
            return;
        }

        int success = 0;
        double totalExec = 0;
        for (Cloudlet cl : cloudlets) {
            if (cl.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
                success++;
            }
            totalExec += cl.getActualCPUTime();
        }
        double successRate = (double) success / cloudlets.size() * 100.0;

        // CPU utilization estimate
        int totalMipsCapacity = 0;
        for (Datacenter dc : datacenters) {
            List<Host> hostList =  dc.getHostList();
            for (Host host : hostList) {
                totalMipsCapacity += host.getPeList().size() * HOST_PE_MIPS;
            }
        }
        double avgMipsUsed = (totalExec * VM_MIPS) / Math.max(1.0, simWallTime);
        double cpuUtilPercent = (avgMipsUsed / Math.max(1.0, totalMipsCapacity)) * 100.0;

        Log.printLine("\n════════════════ SUMMARY ════════════════");
        Log.printLine("Total cloudlets:          " + cloudlets.size());
        Log.printLine("Successful cloudlets:     " + success + " (" + DF.format(successRate) + "%)");
        Log.printLine("Total execution time sum: " + DF.format(totalExec) + " seconds");
        Log.printLine("Simulation time:          " + DF.format(simWallTime) + " seconds");
        Log.printLine("Approx CPU utilization:   " + DF.format(cpuUtilPercent) + " %");

        // Cost per task - read from file
        double totalCost = 0.0;
        try (Scanner sc = new Scanner(new java.io.File("cost_metrics.csv"))) {
            sc.nextLine(); // header
            while (sc.hasNextLine()) {
                String[] parts = sc.nextLine().split(",");
                if (parts.length == 2 && parts[0].equals("Total_Cost")) {
                    totalCost = Double.parseDouble(parts[1]);
                    break;
                }
            }
        } catch (Exception ignored) {}

        double costPerCloudlet = totalCost / cloudlets.size();

        Log.printLine("Total cost (approx):      $" + DF.format(totalCost));
        Log.printLine("Cost per cloudlet:        $" + DF.format(costPerCloudlet));
        Log.printLine("═════════════════════════════════════════");
    }
}
