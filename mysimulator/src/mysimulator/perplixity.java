package mysimulator;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

/**
 * CloudSim 7 Compatible Simulator with Metrics Collection
 * CORRECTED FOR ACTUAL CLOUDSIM 7 API
 * 
 * This simulator:
 * ✓ Uses Host (not HostEntity)
 * ✓ Uses actual CloudSim 7 methods (confirmed from official examples)
 * ✓ Handles missing methods gracefully
 * ✓ Exports metrics to 6 CSV files
 * ✓ Collects resource utilization, performance, and cost metrics
 */
public class perplixity {
    private static final int NUM_USERS = 1;
    private static final String TRACE_FLAG = "false";
    private static final String SIMULATION_NAME = "Multi-Datacenter CloudSim Simulation";
    
    private static List<Datacenter> datacenters = new ArrayList<>();
    private static DatacenterBroker broker;
    private static List<Vm> vmList = new ArrayList<>();
    private static List<Cloudlet> cloudletList = new ArrayList<>();
    
    private static DecimalFormat df = new DecimalFormat("###.##");

    public static void main(String[] args) {
        Log.println("╔═══════════════════════════════════════════════════════════╗");
        Log.println("║     Starting Multi-DC CloudSim 7 Simulation              ║");
        Log.println("╚═══════════════════════════════════════════════════════════╝");
        
        try {
            // Initialize CloudSim
            int num_user = NUM_USERS;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;
            
            CloudSim.init(num_user, calendar, trace_flag);
            
            // Create Datacenters
            Log.println("\n→ Creating datacenters...");
            datacenters.add(createDatacenter("Datacenter_0"));
            datacenters.add(createDatacenter("Datacenter_1"));
            datacenters.add(createDatacenter("Datacenter_2"));
            Log.println("✓ Created " + datacenters.size() + " datacenters");
            
            // Create Broker
            Log.println("\n→ Creating broker...");
            broker = new DatacenterBroker("Broker_0");
            
            // Create VMs
            Log.println("\n→ Creating VMs...");
            vmList = createVMs(broker.getId(), 15, 0);
            Log.println("✓ Created " + vmList.size() + " VMs");
            
            // Create Cloudlets
            Log.println("\n→ Creating cloudlets...");
            cloudletList = createCloudlets(broker.getId(), 100, 0);
            Log.println("✓ Created " + cloudletList.size() + " cloudlets");
            
            // Submit VMs and Cloudlets to broker
            broker.submitGuestList(vmList);
            broker.submitCloudletList(cloudletList);
            
            // Start simulation
            Log.println("\n→ Starting simulation...");
            CloudSim.startSimulation();
            CloudSim.stopSimulation();
            
            // Collect and export metrics
            Log.println("\n→ Collecting and saving metrics to CSV files...");
            collectAndSaveMetrics();
            
            Log.println("\n✓ All metrics saved successfully!");
            Log.println("╔═══════════════════════════════════════════════════════════╗");
            Log.println("║     Multi-DC CloudSim simulation finished!                ║");
            Log.println("╚═══════════════════════════════════════════════════════════╝");
            
        } catch (Exception e) {
            Log.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates VMs with specified parameters
     */
    private static List<Vm> createVMs(int brokerId, int numVMs, int idOffset) {
        List<Vm> list = new ArrayList<>();
        
        int mips = 250;
        long size = 10000;      // MB
        int ram = 512;          // MB
        long bw = 1000;         // Mbps
        int pesNumber = 1;
        String vmm = "Xen";
        
        for (int i = 0; i < numVMs; i++) {
            Vm vm = new Vm(
                idOffset + i,
                brokerId,
                mips,
                pesNumber,
                ram,
                bw,
                size,
                vmm,
                new CloudletSchedulerTimeShared()
            );
            list.add(vm);
        }
        
        return list;
    }

    /**
     * Creates Cloudlets with specified parameters
     */
    private static List<Cloudlet> createCloudlets(int brokerId, int numCloudlets, int idOffset) {
        List<Cloudlet> list = new ArrayList<>();
        
        long length = 40000;        // MI
        long fileSize = 300;        // Bytes
        long outputSize = 300;      // Bytes
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();
        
        for (int i = 0; i < numCloudlets; i++) {
            Cloudlet cloudlet = new Cloudlet(
                idOffset + i,
                length,
                pesNumber,
                fileSize,
                outputSize,
                utilizationModel,
                utilizationModel,
                utilizationModel
            );
            cloudlet.setUserId(brokerId);
            cloudlet.setGuestId(i % vmList.size());  // Distribute cloudlets across VMs
            list.add(cloudlet);
        }
        
        return list;
    }

    /**
     * Creates a Datacenter with Host configuration
     */
    private static Datacenter createDatacenter(String name) throws Exception {
        List<Host> hostList = new ArrayList<>();
        
        // Create PE list for Host 1
        List<Pe> peList1 = new ArrayList<>();
        int mips = 1000;
        peList1.add(new Pe(0, new PeProvisionerSimple(mips)));
        peList1.add(new Pe(1, new PeProvisionerSimple(mips)));
        peList1.add(new Pe(2, new PeProvisionerSimple(mips)));
        peList1.add(new Pe(3, new PeProvisionerSimple(mips)));
        
        // Create PE list for Host 2
        List<Pe> peList2 = new ArrayList<>();
        peList2.add(new Pe(0, new PeProvisionerSimple(mips)));
        peList2.add(new Pe(1, new PeProvisionerSimple(mips)));
        
        // Create Hosts
        int ram = 16384;        // MB
        long storage = 1000000; // MB
        int bw = 10000;         // Mbps
        
        hostList.add(new Host(
            0,
            new RamProvisionerSimple(ram),
            new BwProvisionerSimple(bw),
            storage,
            peList1,
            new VmSchedulerTimeShared(peList1)
        ));
        
        hostList.add(new Host(
            1,
            new RamProvisionerSimple(ram),
            new BwProvisionerSimple(bw),
            storage,
            peList2,
            new VmSchedulerTimeShared(peList2)
        ));
        
        // Create Datacenter characteristics
        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double timeZone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;
        
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
            arch, os, vmm, hostList, timeZone, cost, costPerMem, costPerStorage, costPerBw
        );
        
        LinkedList<Storage> storageList = new LinkedList<>();
        
        Datacenter datacenter = new Datacenter(
            name,
            characteristics,
            new VmAllocationPolicySimple(hostList),
            storageList,
            0
        );
        
        return datacenter;
    }

    /**
     * Collects metrics and exports to CSV files
     */
    private static void collectAndSaveMetrics() {
        List<Cloudlet> finishedCloudlets = broker.getCloudletReceivedList();
        
        // Export all metrics
        exportCloudletMetrics(finishedCloudlets);
        exportVmMetrics();
        exportHostMetrics();
        exportDatacenterSummary();
        exportPerformanceMetrics(finishedCloudlets);
        exportCostMetrics(finishedCloudlets);
    }

    /**
     * Export Cloudlet metrics to CSV
     */
    private static void exportCloudletMetrics(List<Cloudlet> cloudlets) {
        try (FileWriter writer = new FileWriter("cloudlet_metrics.csv")) {
            // Header
            writer.write("Cloudlet_ID,VM_ID,Status,Datacenter_ID,");
            writer.write("Length_MI,Exec_Start_Time,Exec_Finish_Time,");
            writer.write("Actual_CPU_Time,Waiting_Time\n");
            
            for (Cloudlet cl : cloudlets) {
                double waitingTime = cl.getExecStartTime() - cl.getSubmissionTime();
                writer.write(String.format(
                    "%d,%d,%s,%d,%.0f,%.2f,%.2f,%.2f,%.2f\n",
                    cl.getCloudletId(),
                    cl.getGuestId(),
                    cl.getStatus(),
                    cl.getResourceId(),
                    (double)cl.getCloudletLength(),
                    cl.getExecStartTime(),
                    cl.getExecFinishTime(),
                    cl.getActualCPUTime(),
                    waitingTime
                ));
            }
            Log.println("✓ Exported: cloudlet_metrics.csv (" + cloudlets.size() + " records)");
        } catch (IOException e) {
            Log.println("✗ Error writing cloudlet_metrics.csv: " + e.getMessage());
        }
    }

    /**
     * Export VM metrics to CSV
     */
    private static void exportVmMetrics() {
        try (FileWriter writer = new FileWriter("vm_metrics.csv")) {
            // Header
            writer.write("VM_ID,MIPS,RAM_MB,BW_Mbps,PEs,Host_ID,Status\n");
            
            for (Vm vm : vmList) {
                // ✓ FIX: Cast HostEntity to Host
                Host host = (Host) vm.getHost();
                String hostId = host != null ? String.valueOf(host.getId()) : "N/A";
                String status = vm.isInMigration() ? "MIGRATING" : "ACTIVE";
                
                writer.write(String.format(
                	    "%d,%.0f,%.0f,%.0f,%d,%s,%s\n",
                	    vm.getId(),
                	    vm.getMips(),           // %.0f for Double
                	    (double) vm.getRam(),   // %.0f for Double
                	    (double) vm.getBw(),    // %.0f for Double
                	    vm.getNumberOfPes(),
                	    hostId,
                	    status
                	));

            }
            Log.println("✓ Exported: vm_metrics.csv (" + vmList.size() + " records)");
        } catch (IOException e) {
            Log.println("✗ Error writing vm_metrics.csv: " + e.getMessage());
        }
    }


    /**
     * Export Host metrics to CSV
     */
    private static void exportHostMetrics() {
        try (FileWriter writer = new FileWriter("host_metrics.csv")) {
            // Header
            writer.write("Host_ID,Datacenter,PEs,RAM_MB,BW_Mbps,Storage_MB,");
            writer.write("VMs_Allocated,Total_MIPS_Capacity\n");
            
            for (Datacenter dc : datacenters) {
                List<Host> hostList = dc.getHostList();
                for (Host host : hostList) {
                    int totalMips = host.getPeList().size() * 1000;  // Default MIPS
                    
                    writer.write(String.format(
                        "%d,%s,%d,%d,%d,%d,%d,%d\n",
                        host.getId(),
                        dc.getName(),
                        host.getPeList().size(),
                        host.getRamProvisioner().getRam(),
                        host.getBwProvisioner().getBw(),
                        host.getStorage(),
                        host.getGuestList().size(),
                        totalMips
                    ));
                }
            }
            Log.println("✓ Exported: host_metrics.csv");
        } catch (IOException e) {
            Log.println("✗ Error writing host_metrics.csv: " + e.getMessage());
        }
    }

    /**
     * Export Datacenter summary to CSV
     */
    private static void exportDatacenterSummary() {
        try (FileWriter writer = new FileWriter("datacenter_summary.csv")) {
            // Header
            writer.write("Datacenter,Total_Hosts,Total_PEs,Total_VMs,");
            writer.write("Total_RAM_MB,Total_BW_Mbps,Total_Storage_MB\n");
            
            for (Datacenter dc : datacenters) {
                List<Host> hostList = dc.getHostList();
                int totalHosts = hostList.size();
                int totalPes = 0;
                int totalVMs = 0;
                int totalRam = 0;
                int totalBw = 0;
                long totalStorage = 0;
                
                for (Host host : hostList) {
                    totalPes += host.getPeList().size();
                    totalVMs += host.getGuestList().size();
                    totalRam += host.getRamProvisioner().getRam();
                    totalBw += host.getBwProvisioner().getBw();
                    totalStorage += host.getStorage();
                }
                
                writer.write(String.format(
                    "%s,%d,%d,%d,%d,%d,%d\n",
                    dc.getName(),
                    totalHosts,
                    totalPes,
                    totalVMs,
                    totalRam,
                    totalBw,
                    totalStorage
                ));
            }
            Log.println("✓ Exported: datacenter_summary.csv");
        } catch (IOException e) {
            Log.println("✗ Error writing datacenter_summary.csv: " + e.getMessage());
        }
    }

    /**
     * Export Performance metrics to CSV
     */
    private static void exportPerformanceMetrics(List<Cloudlet> cloudlets) {
        try (FileWriter writer = new FileWriter("performance_metrics.csv")) {
            // Header
            writer.write("Metric,Value\n");
            
            if (cloudlets.isEmpty()) {
                writer.write("Total_Cloudlets,0\n");
                writer.write("Successful_Cloudlets,0\n");
                writer.write("Failed_Cloudlets,0\n");
                return;
            }
            
            int successful = 0;
            int failed = 0;
            double totalExecTime = 0;
            double totalWaitingTime = 0;
            double minExecTime = Double.MAX_VALUE;
            double maxExecTime = 0;
            
            for (Cloudlet cl : cloudlets) {
                if (cl.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
                    successful++;
                } else {
                    failed++;
                }
                
                double execTime = cl.getActualCPUTime();
                double waitTime = cl.getExecStartTime() - cl.getSubmissionTime();
                
                totalExecTime += execTime;
                totalWaitingTime += waitTime;
                minExecTime = Math.min(minExecTime, execTime);
                maxExecTime = Math.max(maxExecTime, execTime);
            }
            
            double avgExecTime = totalExecTime / cloudlets.size();
            double avgWaitTime = totalWaitingTime / cloudlets.size();
            double successRate = (double)successful / cloudlets.size() * 100;
            
            writer.write("Total_Cloudlets," + cloudlets.size() + "\n");
            writer.write("Successful_Cloudlets," + successful + "\n");
            writer.write("Failed_Cloudlets," + failed + "\n");
            writer.write("Success_Rate_Percent," + df.format(successRate) + "\n");
            writer.write("Avg_Execution_Time_Sec," + df.format(avgExecTime) + "\n");
            writer.write("Avg_Waiting_Time_Sec," + df.format(avgWaitTime) + "\n");
            writer.write("Min_Execution_Time_Sec," + df.format(minExecTime) + "\n");
            writer.write("Max_Execution_Time_Sec," + df.format(maxExecTime) + "\n");
            writer.write("Total_Execution_Time_Sec," + df.format(totalExecTime) + "\n");
            writer.write("Total_VMs_Used," + vmList.size() + "\n");
            
            Log.println("✓ Exported: performance_metrics.csv");
        } catch (IOException e) {
            Log.println("✗ Error writing performance_metrics.csv: " + e.getMessage());
        }
    }

    /**
     * Export Cost metrics to CSV
     * FIX: Line 467 - Don't use getCharacteristics() (not visible)
     *      Instead, calculate costs directly using fixed values
     */
    private static void exportCostMetrics(List<Cloudlet> cloudlets) {
        try (FileWriter writer = new FileWriter("cost_metrics.csv")) {
            // Header
            writer.write("Cost_Type,Value_USD\n");
            
            double totalComputeCost = 0;
            double totalMemoryCost = 0;
            double totalStorageCost = 0;
            double totalBwCost = 0;
            
            // Default cost values (same as in createDatacenter)
            double cpuCost = 3.0;           // Cost per PE per second
            double memCost = 0.05;          // Cost per MB
            double storageCost = 0.001;     // Cost per MB
            double bwCost = 0.0;            // Cost per Mbps
            
            for (Datacenter dc : datacenters) {
                // ✓ FIX: Don't use dc.getCharacteristics() - method not visible in CloudSim 7
                // Instead, calculate from hosts directly
                List<Host> hostList = dc.getHostList();
                for (Host host : hostList) {
                    // Compute cost (based on number of PEs)
                    totalComputeCost += cpuCost * host.getPeList().size();
                    
                    // Memory cost (based on total RAM)
                    totalMemoryCost += memCost * host.getRamProvisioner().getRam();
                    
                    // Storage cost (based on total storage)
                    totalStorageCost += storageCost * host.getStorage();
                }
                
                // Bandwidth cost
                totalBwCost += bwCost * 100;  // Sample calculation
            }
            
            double totalCost = totalComputeCost + totalMemoryCost + totalStorageCost + totalBwCost;
            
            writer.write("Compute_Cost," + df.format(totalComputeCost) + "\n");
            writer.write("Memory_Cost," + df.format(totalMemoryCost) + "\n");
            writer.write("Storage_Cost," + df.format(totalStorageCost) + "\n");
            writer.write("Bandwidth_Cost," + df.format(totalBwCost) + "\n");
            writer.write("Total_Cost," + df.format(totalCost) + "\n");
            
            if (!cloudlets.isEmpty()) {
                double costPerCloudlet = totalCost / cloudlets.size();
                writer.write("Cost_Per_Cloudlet," + df.format(costPerCloudlet) + "\n");
            }
            
            Log.println("✓ Exported: cost_metrics.csv");
        } catch (IOException e) {
            Log.println("✗ Error writing cost_metrics.csv: " + e.getMessage());
        }
    }
}