package com.cloudsim.examples;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmCost;
import org.cloudsimplus.vms.VmSimple;
import org.cloudsimplus.power.models.PowerModelHostSimple;
//import org.cloudsimplus.power.


import java.util.ArrayList;
import java.util.List;

/**
 * Fixed CloudSim Plus Simulation Scenario
 * - 100 cloudlets
 * - 1 broker
 * - 15 VMs
 * - 3 datacenters, each with 1 host containing 6 PE cores
 */
public class simu_advanced {

    private static final int DATACENTERS = 3;
    private static final int HOSTS_PER_DATACENTER = 1;
    private static final int PES_PER_HOST = 5;
    private static final int VMS = 15;
    private static final int CLOUDLETS = 100;

    // Host specifications
    private static final long HOST_MIPS = 1000;
    private static final long HOST_RAM = 32768; // 32 GB
    private static final long HOST_STORAGE = 1000000;
    private static final long HOST_BW = 100000;

    // VM specifications
    private static final long VM_MIPS = 1000;
    private static final int VM_PES = 1;
    private static final long VM_RAM = 4096; // 4 GB
    private static final long VM_STORAGE = 10_000;
    private static final long VM_BW = 10000;

    // Cloudlet specifications
    private static final long CLOUDLET_LENGTH = 1000;
    private static final long CLOUDLET_FILE_SIZE = 300;
    private static final long CLOUDLET_OUTPUT_SIZE = 300;
    private static final int CLOUDLET_PES = 1;

    
 // Cost parameters (example values)
    private static final double COST_PER_SEC      = 3.0;   // CPU cost per second
    private static final double COST_PER_MEM      = 0.005;  // Cost per MB of RAM
    private static final double COST_PER_STORAGE  = 0.001; // Cost per MB of storage
    private static final double COST_PER_BW       = 0.001; // Cost per MB of bandwidth

    // Power  parameters (example values)
    private static final int SCHEDULING_INTERVAL = 10;

    private static final double STATIC_POWER      = 50;   // CPU cost per second
    private static final double MAX_POWER      = 200;  // Cost per MB of RAM
    
   
    private final CloudSimPlus simulation;
    private final DatacenterBroker broker;
    private List<Vm> vmList;
    private List<Cloudlet> cloudletList;
    private List<Datacenter> datacenterList;

    public static void main(String[] args) {
        new simu_advanced();
    }

    private simu_advanced() {
        System.out.println("Starting CloudSim Plus Simulation...");
        System.out.println("Configuration:");
        System.out.println("  Datacenters: " + DATACENTERS);
        System.out.println("  Hosts per datacenter: " + HOSTS_PER_DATACENTER);
        System.out.println("  PE cores per host: " + PES_PER_HOST);
        System.out.println("  Total VMs: " + VMS);
        System.out.println("  Total Cloudlets: " + CLOUDLETS);
        System.out.println();

        // Initialize simulation
        simulation = new CloudSimPlus();

        // Create datacenters
        datacenterList = new ArrayList<>(DATACENTERS);
        for (int i = 0; i < DATACENTERS; i++) {
            Datacenter dc = createDatacenter();
            datacenterList.add(dc);
            System.out.println("Created Datacenter " + dc.getId() + 
                             " with " + HOSTS_PER_DATACENTER + " host(s)");
        }

        // Create broker
        broker = new DatacenterBrokerSimple(simulation);
        System.out.println("\nCreated Broker " + broker.getId());

        // Create VMs
        vmList = createVms();
        System.out.println("Created " + vmList.size() + " VMs");

        // Create cloudlets
        cloudletList = createCloudlets();
        System.out.println("Created " + cloudletList.size() + " Cloudlets");

        // Submit VMs and cloudlets to broker
        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);

        System.out.println("\nStarting simulation...\n");

        // Start simulation
        simulation.start();

        // Print results
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SIMULATION RESULTS");
        System.out.println("=".repeat(80) + "\n");

        List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        new CloudletsTableBuilder(finishedCloudlets).build();

        // Print summary statistics
        printSummaryStatistics(finishedCloudlets);
    }

    private Datacenter createDatacenter() {
        List<Host> hostList = new ArrayList<>(HOSTS_PER_DATACENTER);
        for (int i = 0; i < HOSTS_PER_DATACENTER; i++) {
            hostList.add(createHost());
        }
        DatacenterSimple dc=new DatacenterSimple(simulation, hostList);
        dc.setSchedulingInterval(SCHEDULING_INTERVAL);
        dc.getCharacteristics()
        .setCostPerSecond(COST_PER_SEC)       // Cost per CPU second
        .setCostPerMem(COST_PER_MEM)          // Cost per MB of RAM used
        .setCostPerStorage(COST_PER_STORAGE)  // Cost per MB of storage used
        .setCostPerBw(COST_PER_BW);           // Cost per MB of Bandwidth used
        return dc;
    }


    private Host createHost() {
        List<Pe> peList = new ArrayList<>(PES_PER_HOST);
        for (int i = 0; i < PES_PER_HOST; i++) {
            peList.add(new PeSimple(HOST_MIPS));
        }
        // Use PowerHost instead of HostSimple
        
        HostSimple host = new HostSimple(HOST_RAM, HOST_BW, HOST_STORAGE, peList);
        host.setPowerModel(new PowerModelHostSimple(MAX_POWER, STATIC_POWER));
        host.enableUtilizationStats();
        host.setStartupDelay(0).setShutDownDelay(0); // optional
        return host;
    }


    private List<Vm> createVms() {
        List<Vm> list = new ArrayList<>(VMS);
        for (int i = 0; i < VMS; i++) {
            Vm vm = new VmSimple(VM_MIPS, VM_PES);
            vm.setRam(VM_RAM).setBw(VM_BW).setSize(VM_STORAGE);
            vm.enableUtilizationStats();
           
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            list.add(vm);
        }
        return list;
    }

    private List<Cloudlet> createCloudlets() {
        List<Cloudlet> list = new ArrayList<>(CLOUDLETS);

        // CPU:  usage

        UtilizationModelDynamic cpuModel = new UtilizationModelDynamic(0.8); // 80% CPU
        UtilizationModelDynamic ramModel = new UtilizationModelDynamic(0.5); // 50% RAM
        UtilizationModelDynamic bwModel = new UtilizationModelDynamic(0.3);  // 30% BW
        for (int i = 0; i < CLOUDLETS; i++) {
            Cloudlet cloudlet = new CloudletSimple(CLOUDLET_LENGTH, CLOUDLET_PES);
            
            // FIX: Explicitly set low memory/bandwidth consumption
            cloudlet.setUtilizationModelCpu(new UtilizationModelFull());
            cloudlet.setUtilizationModelRam(new UtilizationModelFull());
            cloudlet.setUtilizationModelBw(new UtilizationModelFull());
            
            cloudlet.setSizes(CLOUDLET_FILE_SIZE);
            list.add(cloudlet);
        }

        return list;
    }

    private void printSummaryStatistics(List<Cloudlet> finishedCloudlets) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("COMPREHENSIVE SIMULATION ANALYSIS");
        System.out.println("=".repeat(80));

        // Section 1: Execution Metrics
        printExecutionMetrics(finishedCloudlets);
        
        // Section 2: Resource Utilization Analysis
        printResourceUtilizationAnalysis();
        
        
        // Section 3: Performance Metrics
        printPerformanceMetrics(finishedCloudlets);
        
        // Section 4: Power & Energy Analysis
        printPowerEnergyAnalysis();
        
       
        // Section 5: NATIVE Cost Analysis (Realistic Cloud Pricing)
        printNativeCostAnalysis(finishedCloudlets);
        
        System.out.println("=".repeat(80));
    }
  
    private void printNativeCostAnalysis(List<Cloudlet> finishedCloudlets) {
        System.out.println("\n## NATIVE CLOUDSIM PLUS COST ANALYSIS");
        System.out.println("-".repeat(80));

        double totalCpuCost = 0.0;
        double totalMemCost = 0.0;
        double totalStorageCost = 0.0;
        double totalBwCost = 0.0;
        double totalVmCost = 0.0;

        List<Vm> allVms = broker.getVmCreatedList();
        
        // Calculate cost using CloudSim Plus built-in VmCost API
        System.out.println("\n  VM-BASED COSTS (VmCost API):");
        System.out.println("  " + "-".repeat(78));
        System.out.printf("  %-8s %-12s %-12s %-14s %-12s %-12s%n", 
            "VM ID", "CPU Cost", "Mem Cost", "Storage Cost", "BW Cost", "Total Cost");
        System.out.println("  " + "-".repeat(78));

        for (Vm vm : allVms) {
            // CloudSim Plus native cost calculation using VmCost
            VmCost vmCost = new VmCost(vm);
            
            double cpuCost = vmCost.getProcessingCost();
            double memCost = vmCost.getMemoryCost();
            double storageCost = vmCost.getStorageCost();
            double bwCost = vmCost.getBwCost();
            double totalCost = vmCost.getTotalCost();
            
            totalCpuCost += cpuCost;
            totalMemCost += memCost;
            totalStorageCost += storageCost;
            totalBwCost += bwCost;
            totalVmCost += totalCost;
            
            // Print first 5 and last 2 VMs
            if (vm.getId() < 5 || vm.getId() >= allVms.size() - 2) {
                System.out.printf("  %-8d $%-11.4f $%-11.4f $%-13.4f $%-11.4f $%-11.4f%n",
                    vm.getId(), cpuCost, memCost, storageCost, bwCost, totalCost);
            } else if (vm.getId() == 5) {
                System.out.println("  ...");
            }
        }
        
        System.out.println("  " + "-".repeat(78));
        System.out.printf("  TOTAL:   $%-11.4f $%-11.4f $%-13.4f $%-11.4f $%-11.4f%n",
            totalCpuCost, totalMemCost, totalStorageCost, totalBwCost, totalVmCost);

        // Cost summary
        System.out.println("\n  COST SUMMARY:");
        System.out.println("  " + "-".repeat(78));
        System.out.printf("    Total Infrastructure Cost:              $%.4f%n", totalVmCost);
        System.out.printf("      ├─ CPU Processing Cost:               $%.4f (%.1f%%)%n", 
            totalCpuCost, (totalCpuCost / totalVmCost) * 100);
        System.out.printf("      ├─ Memory Cost:                       $%.4f (%.1f%%)%n", 
            totalMemCost, (totalMemCost / totalVmCost) * 100);
        System.out.printf("      ├─ Storage Cost:                      $%.4f (%.1f%%)%n", 
            totalStorageCost, (totalStorageCost / totalVmCost) * 100);
        System.out.printf("      └─ Bandwidth Cost:                    $%.4f (%.1f%%)%n", 
            totalBwCost, (totalBwCost / totalVmCost) * 100);
        System.out.println();
        System.out.printf("    Cost per VM (average):                  $%.4f%n", 
            totalVmCost / allVms.size());
        System.out.printf("    Cost per Cloudlet (average):            $%.6f%n", 
            totalVmCost / finishedCloudlets.size());
        System.out.printf("    Cost per Second:                        $%.6f%n", 
            totalVmCost / simulation.clock());
        
        // Additional metrics
        double simTime = simulation.clock();
        System.out.println("\n  COST EFFICIENCY METRICS:");
        System.out.println("  " + "-".repeat(78));
        System.out.printf("    Simulation Duration:                    %.2f seconds%n", simTime);
        System.out.printf("    Total VMs:                              %d%n", allVms.size());
        System.out.printf("    Total Cloudlets:                        %d%n", finishedCloudlets.size());
        System.out.printf("    Cost per VM-Second:                     $%.6f%n", 
            totalVmCost / (allVms.size() * simTime));
        System.out.printf("    Revenue per Cloudlet (if charging $1):  %.1f%% margin%n",
            ((1.0 - (totalVmCost / finishedCloudlets.size())) * 100));
    }

    // ==================== SECTION 1: Execution Metrics ====================
    private void printExecutionMetrics(List<Cloudlet> finishedCloudlets) {
        System.out.println("\n## 1. EXECUTION METRICS");
        System.out.println("-".repeat(80));
        
        int totalCloudlets = CLOUDLETS;
        int finishedCount = finishedCloudlets.size();
        double successRate = (finishedCount * 100.0) / totalCloudlets;
        
        double totalExecTime = finishedCloudlets.stream()
            .mapToDouble(c -> (c.getFinishTime() - c.getStartTime()))
            .sum();
        double avgExecTime = finishedCount > 0 ? totalExecTime / finishedCount : 0;
        double minExecTime = finishedCloudlets.stream()
            .mapToDouble(c -> (c.getFinishTime() - c.getStartTime()))
            .min().orElse(0);
        double maxExecTime = finishedCloudlets.stream()
            .mapToDouble(c -> (c.getFinishTime() - c.getStartTime()))
            .max().orElse(0);
        
        double simDuration = simulation.clock();
        double throughput = finishedCount / simDuration; // cloudlets/second
        
        System.out.printf("  Total Cloudlets: %d | Success: %d | Failed: %d | Success Rate: %.1f%%%n",
            totalCloudlets, finishedCount, totalCloudlets - finishedCount, successRate);
        System.out.printf("  Execution Time - Avg: %.3fs | Min: %.3fs | Max: %.3fs%n",
            avgExecTime, minExecTime, maxExecTime);
        System.out.printf("  Simulation Duration: %.2f seconds%n", simDuration);
        System.out.printf("  Throughput: %.2f cloudlets/second%n", throughput);
    }

    // ==================== SECTION 2: Resource Utilization ====================
    private void printResourceUtilizationAnalysis() {
        System.out.println("\n## 2. RESOURCE UTILIZATION ANALYSIS");
        System.out.println("-".repeat(80));
        
        List<Vm> allVms = broker.getVmCreatedList();
        
        // Host-level analysis
        System.out.println("\n  A) HOST-LEVEL UTILIZATION:");
        System.out.println("  " + "-".repeat(78));
        System.out.println(String.format("  %-12s %-6s %-12s %-12s %-12s %-8s",
            "Datacenter", "Host", "CPU Util", "VMs", "Cloudlets", "Power"));
        System.out.println("  " + "-".repeat(78));
        
        double totalCpuUtil = 0.0;
        int totalHosts = 0;
        int totalVmsCreated = 0;
        
        for (Datacenter dc : datacenterList) {
            for (Host host : dc.getHostList()) {
                double cpuUtil = host.getCpuUtilizationStats().getMean();
                
                // Count VMs on this host
                long vmsOnHost = allVms.stream()
                    .filter(vm -> vm.getHost().getId() == host.getId() && 
                                 vm.getHost().getDatacenter().getId() == dc.getId())
                    .count();
                
                // Count cloudlets executed on this host
                long cloudletsOnHost = broker.getCloudletFinishedList().stream()
                    .filter(c -> c.getVm().getHost().getId() == host.getId() &&
                               c.getVm().getHost().getDatacenter().getId() == dc.getId())
                    .count();
                
                double power = host.getPowerModel().getPower(cpuUtil);
                
                System.out.printf("  %-12s %-6d %10.1f%% %12d %12d %8.1f W%n",
                    "DC-" + dc.getId(), host.getId(), cpuUtil * 100, 
                    vmsOnHost, cloudletsOnHost, power);
                
                totalCpuUtil += cpuUtil;
                totalHosts++;
                totalVmsCreated += vmsOnHost;
            }
        }
        
        System.out.println("  " + "-".repeat(78));
        System.out.printf("  AVERAGE: %44s %10.1f%%%n", "", (totalCpuUtil / totalHosts) * 100);
        
        // VM-level analysis
        System.out.println("\n  B) VM-LEVEL UTILIZATION:");
        System.out.println("  " + "-".repeat(78));
        System.out.println(String.format("  %-8s %-12s %-12s %-12s %-12s",
            "VM ID", "CPU Util", "RAM Alloc", "BW Alloc", "Cloudlets"));
        System.out.println("  " + "-".repeat(78));
        
        double totalVmCpuUtil = 0.0;
        long totalRamUsed = 0;
        long totalBwUsed = 0;
        
        for (Vm vm : allVms) {
            double vmCpuUtil = vm.getCpuUtilizationStats().getMean();
            
            // Calculate actual resource usage
            long ramUsed = (long)(vm.getRam().getCapacity() * 1.0); // 100% allocated
            long bwUsed = (long)(vm.getBw().getCapacity() * 1.0);   // 100% allocated
            
            long cloudletCount = broker.getCloudletFinishedList().stream()
                .filter(c -> c.getVm().getId() == vm.getId())
                .count();
            
            if (vm.getId() < 5 || vm.getId() >= allVms.size() - 2) { // Show first 5 and last 2
                System.out.printf("  %-8d %10.1f%% %10d MB %10d Mbps %12d%n",
                    vm.getId(), vmCpuUtil * 100, ramUsed, bwUsed, cloudletCount);
            } else if (vm.getId() == 5) {
                System.out.println("  ...");
            }
            
            totalVmCpuUtil += vmCpuUtil;
            totalRamUsed += ramUsed;
            totalBwUsed += bwUsed;
        }
        
        System.out.println("  " + "-".repeat(78));
        System.out.printf("  AVERAGE: %10.1f%% %10d MB %10d Mbps%n",
            (totalVmCpuUtil / allVms.size()) * 100,
            totalRamUsed / allVms.size(),
            totalBwUsed / allVms.size());
    }

    // ==================== SECTION 3: Power & Energy ====================
    private void printPowerEnergyAnalysis() {
        System.out.println("\n## 3. POWER & ENERGY CONSUMPTION");
        System.out.println("-".repeat(80));
        
        double simTime = simulation.clock();
        double totalPower = 0.0;
        
        System.out.println(String.format("\n  %-15s %-15s %-15s %-15s",
            "Datacenter", "Avg Power (W)", "Energy (J)", "Energy (kWh)"));
        System.out.println("  " + "-".repeat(76));
        
        for (Datacenter dc : datacenterList) {
            double dcPower = 0.0;
            for (Host host : dc.getHostList()) {
                double cpuUtil = host.getCpuUtilizationStats().getMean();
                dcPower += host.getPowerModel().getPower(cpuUtil);
            }
            
            double dcEnergy = dcPower * simTime;
            double dcEnergyKwh = dcEnergy / 3600000.0;
            
            System.out.printf("  %-15s %15.1f %15.1f %15.6f%n",
                "DC-" + dc.getId(), dcPower, dcEnergy, dcEnergyKwh);
            
            totalPower += dcPower;
        }
        
        double totalEnergy = totalPower * simTime;
        double totalEnergyKwh = totalEnergy / 3600000.0;
        
        System.out.println("  " + "-".repeat(76));
        System.out.printf("  %-15s %15.1f %15.1f %15.6f%n",
            "TOTAL", totalPower, totalEnergy, totalEnergyKwh);
        
        // Carbon footprint (assuming 0.5 kg CO2/kWh - typical grid average)
        double carbonFootprint = totalEnergyKwh * 0.5;
        System.out.printf("\n  Carbon Footprint: %.6f kg CO2 (grid average: 0.5 kg CO2/kWh)%n", 
            carbonFootprint);
        
        // Energy efficiency
        int finishedCloudlets = broker.getCloudletFinishedList().size();
        if (finishedCloudlets > 0) {
            double energyPerCloudlet = totalEnergy / finishedCloudlets;
            System.out.printf("  Energy Efficiency: %.2f J/cloudlet%n", energyPerCloudlet);
        }
    }


    // ==================== SECTION 5: Performance Metrics ====================
    private void printPerformanceMetrics(List<Cloudlet> finishedCloudlets) {
        System.out.println("\n## 5. PERFORMANCE METRICS");
        System.out.println("-".repeat(80));
        
        double simTime = simulation.clock();
        List<Vm> allVms = broker.getVmCreatedList();
        
        // Resource efficiency
        double avgCpuUtil = allVms.stream()
            .mapToDouble(vm -> vm.getCpuUtilizationStats().getMean())
            .average().orElse(0.0);
        
        // Calculate makespan (time from first cloudlet start to last finish)
        double firstStart = finishedCloudlets.stream()
            .mapToDouble(Cloudlet::getStartTime)
            .min().orElse(0.0);
        double lastFinish = finishedCloudlets.stream()
            .mapToDouble(Cloudlet::getFinishTime)
            .max().orElse(0.0);
        double makespan = lastFinish - firstStart;
        
        // Calculate total MIPS executed
        long totalMI = finishedCloudlets.stream()
            .mapToLong(Cloudlet::getLength)
            .sum();
        double effectiveMIPS = totalMI / simTime;
        
        // Calculate resource wastage
        int totalPEs = DATACENTERS * HOSTS_PER_DATACENTER * PES_PER_HOST;
        double theoreticalMaxMIPS = totalPEs * HOST_MIPS;
        double utilization = effectiveMIPS / theoreticalMaxMIPS;
        
        System.out.printf("  Average CPU Utilization: %.1f%%%n", avgCpuUtil * 100);
        System.out.printf("  Resource Utilization: %.1f%%%n", utilization * 100);
        System.out.printf("  Makespan: %.2f seconds%n", makespan);
        System.out.printf("  Effective MIPS: %.0f MIPS%n", effectiveMIPS);
        System.out.printf("  Total MI Executed: %d MI%n", totalMI);
        System.out.printf("  VM-to-Host Ratio: %.2f%n", (double) allVms.size() / (DATACENTERS * HOSTS_PER_DATACENTER));
        System.out.printf("  Cloudlet-to-VM Ratio: %.2f%n", (double) finishedCloudlets.size() / allVms.size());
    }


}
