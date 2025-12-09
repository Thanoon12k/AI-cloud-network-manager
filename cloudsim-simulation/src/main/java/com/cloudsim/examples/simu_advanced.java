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
import org.cloudsimplus.vms.VmSimple;

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
        return new DatacenterSimple(simulation, hostList);
    }

    private Host createHost() {
        List<Pe> peList = new ArrayList<>(PES_PER_HOST);
        for (int i = 0; i < PES_PER_HOST; i++) {
            peList.add(new PeSimple(HOST_MIPS));
        }
        return new HostSimple(HOST_RAM, HOST_BW, HOST_STORAGE, peList);
    }

    private List<Vm> createVms() {
        List<Vm> list = new ArrayList<>(VMS);
        for (int i = 0; i < VMS; i++) {
            Vm vm = new VmSimple(VM_MIPS, VM_PES);
            vm.setRam(VM_RAM).setBw(VM_BW).setSize(VM_STORAGE);
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            list.add(vm);
        }
        return list;
    }

    private List<Cloudlet> createCloudlets() {
        List<Cloudlet> list = new ArrayList<>(CLOUDLETS);

        // CPU: 50% usage
//        UtilizationModelDynamic cpuModel = new UtilizationModelDynamic(0.5);
        
        // RAM/BW: 1% usage (Allows 100 concurrent cloudlets per VM if needed)
//        UtilizationModelDynamic memoryModel = new UtilizationModelDynamic(0.01);
//        UtilizationModelDynamic bwModel = new UtilizationModelDynamic(0.01);

        for (int i = 0; i < CLOUDLETS; i++) {
            Cloudlet cloudlet = new CloudletSimple(CLOUDLET_LENGTH, CLOUDLET_PES, new UtilizationModelFull());
            
            // FIX: Explicitly set low memory/bandwidth consumption
            cloudlet.setUtilizationModelRam( new UtilizationModelFull());
            cloudlet.setUtilizationModelBw( new UtilizationModelFull());
            
            cloudlet.setSizes(CLOUDLET_FILE_SIZE);
            list.add(cloudlet);
        }

        return list;
    }

    private void printSummaryStatistics(List<Cloudlet> finishedCloudlets) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SUMMARY STATISTICS");
        System.out.println("=".repeat(80));

        int totalCloudlets = CLOUDLETS;
        int finishedCount = finishedCloudlets.size();
        int failedCount = totalCloudlets - finishedCount;

        System.out.println("\nCloudlet Execution:");
        System.out.println("  Total Cloudlets: " + totalCloudlets);
        System.out.println("  Successfully Finished: " + finishedCount);
        System.out.println("  Failed/Incomplete: " + failedCount);
        System.out.println("  Success Rate: " + 
            String.format("%.2f%%", (finishedCount * 100.0 / totalCloudlets)));

        if (!finishedCloudlets.isEmpty()) {
            double totalExecTime = finishedCloudlets.stream()
                .mapToDouble(c ->(c.getFinishTime() - c.getStartTime()))
                .sum();
            double avgExecTime = totalExecTime / finishedCloudlets.size();

            double minExecTime = finishedCloudlets.stream()
                .mapToDouble(c -> (c.getFinishTime() - c.getStartTime()))
                .min()
                .orElse(0);

            double maxExecTime = finishedCloudlets.stream()
                .mapToDouble(c -> (c.getFinishTime() - c.getStartTime()))
                .max()
                .orElse(0);

            System.out.println("\nExecution Time Statistics:");
            System.out.println("  Average Execution Time: " + 
                String.format("%.2f seconds", avgExecTime));
            System.out.println("  Min Execution Time: " + 
                String.format("%.2f seconds", minExecTime));
            System.out.println("  Max Execution Time: " + 
                String.format("%.2f seconds", maxExecTime));
            System.out.println("  Total Execution Time: " + 
                String.format("%.2f seconds", totalExecTime));
        }

        System.out.println("\nVM Statistics:");
        System.out.println("  Total VMs Requested: " + VMS);
        System.out.println("  VMs Successfully Created: " + 
            broker.getVmCreatedList().size());
        System.out.println("  VMs Failed to Create: " + 
            (VMS - broker.getVmCreatedList().size()));

        // NEW: VM Statistics per Datacenter
        System.out.println("\nVMs per Datacenter:");
        for (int i = 0; i < datacenterList.size(); i++) {
            Datacenter dc = datacenterList.get(i);
            int vmCountInDC = 0;
            
            // Count VMs in this datacenter
            for (Vm vm : broker.getVmCreatedList()) {
                if (vm.getHost().getDatacenter().getId() == dc.getId()) {
                    vmCountInDC++;
                }
            }
            
            System.out.println("  Datacenter " + (i + 1) + ": " + vmCountInDC + " VMs");
            
            // Print details of each VM in this DC
            for (Vm vm : broker.getVmCreatedList()) {
                if (vm.getHost().getDatacenter().getId() == dc.getId()) {
                    long vmRam = vm.getRam().getCapacity();
                    long vmBw = vm.getBw().getCapacity();
                    System.out.println("    - VM " + vm.getId() + ": RAM=" + vmRam + "MB, BW=" + vmBw + "Mbps");
                }
            }
        }

        System.out.println("\nDatacenter Configuration:");
        System.out.println("  Number of Datacenters: " + DATACENTERS);
        System.out.println("  Hosts per Datacenter: " + HOSTS_PER_DATACENTER);
        System.out.println("  PE Cores per Host: " + PES_PER_HOST);
        System.out.println("  Total PE Cores: " + (DATACENTERS * HOSTS_PER_DATACENTER * PES_PER_HOST));

        System.out.println("\n" + "=".repeat(80));
    }

}
