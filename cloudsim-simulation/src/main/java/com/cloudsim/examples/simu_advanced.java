package com.cloudsim.examples;

import org.cloudsimplus.allocationpolicies.VmAllocationPolicySimple;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;
import org.cloudsimplus.core.CloudSimPlus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class simu_advanced {
	 private static final int  HOSTS = 1;
	    private static final int  HOST_PES = 5;
	    private static final int  HOST_MIPS = 1000; // Million Instructions per Second (MIPS)
	    private static final int  HOST_RAM = 4096; //in Megabytes
	    private static final long HOST_BW = 10_000; //in Megabits/s
	    private static final long HOST_STORAGE = 50_000; //in Megabytes
	    
	    private static final int  NUM_VMS = 3;
	    private static final int  VM_PES = 1;
	    private static final int  VM_MIPS = 1000; // Million Instructions per Second (MIPS)
	    private static final int  VM_RAM = 512; //in Megabytes
	    private static final long VM_BW = 1000; //in Megabits/s
	    private static final long VM_STORAGE = 10_000; //in Megabytes
	    
	    
	    private static final long NUM_CLOUDLETS = 25; //in Megabytes
	    

    public static void main(String[] args) {
        // Step 1: Initialize simulation
        var simulation = new CloudSimPlus();
        List<Pe> host_cores_list = new ArrayList<>();
    	
    	// Add PEs (cores) to the host
    	for (int i = 0; i < HOST_PES; i++) {
    		host_cores_list.add( new PeSimple(HOST_MIPS) );
    	}
        // Step 2: Create datacenter with hosts
        var host = new HostSimple(4096, 10000, 50000,host_cores_list);
        
        var datacenter1 = new DatacenterSimple(simulation, 
                List.of(host));
        var datacenter2 = new DatacenterSimple(simulation, 
                List.of(host));
        var datacenter3 = new DatacenterSimple(simulation, 
                List.of(host));
            
        // Step 3: Create broker
        var broker = new DatacenterBrokerSimple(simulation);
        
        // Step 4: Create VMs
        List<VmSimple> vmList = new ArrayList<>();
        for (int i = 0; i < NUM_VMS; i++) {
            var vm = new VmSimple(VM_MIPS, 1);
            vm.setRam(VM_RAM).setBw(VM_BW).setSize(VM_STORAGE);
            vmList.add(vm);
        }
        
        // Step 5: Create Cloudlets
        List<CloudletSimple> cloudletList = new ArrayList<>();
        var utilizationModel = new UtilizationModelFull();
        Random random = new Random(42);
        
        for (int i = 0; i < NUM_CLOUDLETS; i++) {
            long length = 1000 + random.nextInt(9001);
            var cloudlet = new CloudletSimple(length, 1, 
                utilizationModel);
            cloudletList.add(cloudlet);
        }
        
        // Step 6: Submit VMs and Cloudlets
        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);
        
        // Step 7: Add listener for power monitoring
//        simulation.addOnClockTickListener(evt -> {
//            for (Host hh : datacenter1.getHostList()) {
//                double util = hh.getCpuPercentUtilization();
//                if (util > 0) {
//                    System.out.println("Time: " + 
//                        simulation.clock() + " Util: " + util);
//                }
//            }
//        });
//        simulation.addOnClockTickListener(evt -> {
//            for (Host hh : datacenter2.getHostList()) {
//                double util = hh.getCpuPercentUtilization();
//                if (util > 0) {
//                    System.out.println("Time: " + 
//                        simulation.clock() + " Util: " + util);
//                }
//            }
//        });
//        simulation.addOnClockTickListener(evt -> {
//            for (Host hh : datacenter3.getHostList()) {
//                double util = hh.getCpuPercentUtilization();
//                if (util > 0) {
//                    System.out.println("Time: " + 
//                        simulation.clock() + " Util: " + util);
//                }
//            }
//        });
        
        // Step 8: Start simulation
        simulation.start();
        
        // Step 9: Print results
        new CloudletsTableBuilder(
            broker.getCloudletFinishedList()).build();
    }
}
