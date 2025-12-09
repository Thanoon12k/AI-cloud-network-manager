package mysimulator;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
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
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class mysim1 {
	public static DatacenterBroker broker;

	private static List<Cloudlet> cloudletList=new ArrayList<Cloudlet>();

	private static List<Vm> vmlist=new ArrayList<Vm>();
		private static  int NUM_USERS=1;
		@SuppressWarnings("unused")
		private static  int NUM_DCS=3;
		private static  int NUM_VMS=15;
		private static  int NUM_CLOUDLETS=10;
//		private static  int NUM_CLOUDLETS=10;
//		private static  int NUM_CLOUDLETS=10;
//		
	
	public static void main(String[] args) {
		Log.println("Starting CloudSimExample6...");
		
		try {
		
	
	
			boolean trace_flag = false;  // mean trace events

			CloudSim.init(NUM_USERS, Calendar.getInstance(), trace_flag);

			// First step: Create a DatacenterBroker
			broker = new DatacenterBroker("Broker");;
			int brokerId = broker.getId();
			// Datacenter specifications
			int host_mips=1000;  //* num_pes;
			int host_pes=5;
			int host_ram = 4096; // host memory (MB)
			long host_storage = 50000; // host storage (MB)
			int host_bw = 10000; // host bandwidth (MBps)
			//Second step: Create Datacenters
			PowerDatacenter datacenter3= createPowerDatacenter("power_Datacenter_3",host_pes,host_mips,host_ram,host_storage,host_bw);
			PowerDatacenter datacenter2= createPowerDatacenter("power_Datacenter_2",5,host_mips,host_ram,host_storage,host_bw);
			PowerDatacenter datacenter1= createPowerDatacenter("power_Datacenter_1",5,host_mips,host_ram,host_storage,host_bw);
//			Datacenter datacenter3= createDatacenter("Datacenter_3",5,host_mips,host_ram,host_storage,host_bw);
//			Datacenter datacenter2= createDatacenter("Datacenter_2",5,host_mips,host_ram,host_storage,host_bw);
//			Datacenter datacenter1= createDatacenter("Datacenter_1",5,host_mips,host_ram,host_storage,host_bw);
			//Datacenters are the resource providers in CloudSim. We need at least one of them to run a CloudSim simulation
			//Third step: Create Broker
			
			//Fourth step: Create VMs and Cloudlets and send them to broker
			//vms_spesifications
			int vm_mips = 1000; // MIPS of each VM
			int vm_ram = 512; // VM memory (MB)
			long vm_storage = 10000; // VM storage (MB)
			int vm_bw = 1000; // VM bandwidth (MBps)
			// Create VMs and Cloudlets
			// We will create 3 VMs and 40 Cloudlets
			vmlist = createVMs(brokerId,NUM_VMS,vm_mips,vm_ram,vm_storage,vm_bw); // creating 3 VMs
			// cloudlet_spesifications
			int min_length=1000;
			int max_length=10000;
			cloudletList = createCloudlets(brokerId,NUM_CLOUDLETS,min_length,max_length); // creating 40 Cloudlets
			
			broker.submitGuestList(vmlist);
			broker.submitCloudletList(cloudletList);

			// Fifth step: Starts the simulation
			CloudSim.startSimulation();

			// Final step: Print results when simulation is over
			List<Cloudlet> newList = broker.getCloudletReceivedList();

			CloudSim.stopSimulation();

			printCloudletList(newList);
//			System.out.println("Energy consumed by " + datacenter1.getName() + ": " + datacenter1.getPower() + " kWh");
//			System.out.println("Energy consumed by " + datacenter2.getName() + ": " + datacenter2.getPower() + " kWh");
//			System.out.println("Energy consumed by " + datacenter3.getName() + ": " + datacenter3.getPower() + " kWh");

			Log.println("CloudSimExample6 finished!");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Log.println("The simulation has been terminated due to an unexpected error");
		}
	}



	
/**
	 * Prints the Cloudlet objects
	 * @param int userId
	 * @param int num_vms
	 * @param int vm_mips
	 * @param int vm_ram
	 * @param long vm_storage
	 * @param int vm_bw
	 * @return List<Vm>
	 */

	private static List<Vm> createVMs(int userId, final int num_vms,int vm_mips,int vm_ram,long vm_storage,int vm_bw) {
		List<Vm> list = new ArrayList<>();

		//		long size = 10000; //image size (MB)
		//		int ram = 512; //vm memory (MB)
		//		int mips = 1000;
		//		long bw = 1000;
		CloudletSchedulerSpaceShared space_scheduler=new CloudletSchedulerSpaceShared();
		CloudletSchedulerTimeShared time_scheduler=new CloudletSchedulerTimeShared();
		for(int i=0;i<num_vms;i++){
			Vm new_vm=new Vm(i, userId, vm_mips, 1, vm_ram, vm_bw, vm_storage, "Xen", space_scheduler);
			list.add(new_vm);
		}

		return list;
	}

		/**
		 * Creates a list of Cloudlets
		 * @param int userId
		 * @param int cloudlets
		 * @param int max_length
		 * @param int min_length
		 * @return List<Cloudlet>
		 */
	private static List<Cloudlet> createCloudlets(int userId, int num_cloudlets, int min_length, int max_length) {
		List<Cloudlet> list = new ArrayList<>();
		Random random = new Random(42);

		long fileSize = 300;
		long outputSize = 300;

    	for (int i = 0; i < num_cloudlets; i++) {
        // Generate random length between min_length and max_length (inclusive)
        long length = min_length + random.nextInt(max_length - min_length + 1);

        // Cloudlet parameters: (id, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel)
        Cloudlet cloudlet = new Cloudlet(
            i, 
            length, 
            1, // number of PEs (processing elements)
            fileSize, 
            outputSize, 
            new UtilizationModelFull(), 
            new UtilizationModelFull(), 
            new UtilizationModelFull()
				);
				cloudlet.setUserId(userId);
				list.add(cloudlet);
			}

			return list;
}



/**
 * Creates a Datacenter with the given specifications
 * 
 * @param String name
 * @param int    num_DCs
 * @param int    host_mips
 * @param int    host_ram
 * @param long   host_storage
 * @param int    host_bw
 * @return Datacenter
 */
private static Datacenter createDatacenter(String name,int num_PEs, int host_mips, int host_ram, long host_storage, int host_bw) {
	// DC_AND_HOST_specifications with 1 host and 5 cores
	List<Host> hostList = new ArrayList<>();
	List<Pe> peList1 = new ArrayList<>();
	
	// Add PEs (cores) to the host
	for (int i = 0; i < num_PEs; i++) {
		peList1.add(new Pe(i, new PeProvisionerSimple(host_mips)));
	}

	// Create Host with 5 cores
	int hostId = 0;
	hostList.add(new Host(hostId, new RamProvisionerSimple(host_ram), new BwProvisionerSimple(host_bw), host_storage,
			peList1, new VmSchedulerTimeShared(peList1)));

	String arch = "x86"; // system architecture
	String os = "Linux"; // operating system
	String vmm = "Xen";
	double time_zone = 10.0; // time zone this resource located
	double costPerCpu = 3.0; // the cost of using processing in this resource
	double costPerMem = 0.05; // the cost of using memory in this resource
	double costPerStorage = 0.1; // the cost of using storage in this resource
	double costPerBw = 0.1; // the cost of using bw in this resource
	LinkedList<Storage> storageList = new LinkedList<>(); // we are not adding SAN devices by now

	DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone,
			costPerCpu, costPerMem, costPerStorage, costPerBw);

	// 6. Finally, we need to create a PowerDatacenter object.
	Datacenter datacenter = null;
	try {
		datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
	} catch (Exception e) {
		e.printStackTrace();
	}

	return datacenter;
}

/**
 * Creates a power Datacenter with the given specifications
 * 
 * @param String name
 * @param int    num_DCs
 * @param int    host_mips
 * @param int    host_ram
 * @param long   host_storage
 * @param int    host_bw
 * @return PowerDatacenter
 */

private static PowerDatacenter createPowerDatacenter(String name, int num_PEs, 
    int host_mips, int host_ram, long host_storage, int host_bw) {
    
    List<PowerHost> hostList = new ArrayList<>();
    
    for (int hostId = 0; hostId < 1; hostId++) {
        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < num_PEs; i++) {
            peList.add(new Pe(i, new PeProvisionerSimple(host_mips)));
        }
        
        PowerHost host = new PowerHost(hostId,
            new RamProvisionerSimple(host_ram),
            new BwProvisionerSimple(host_bw),
            host_storage,
            peList,
            new VmSchedulerTimeShared(peList), 
            new PowerModelLinear(200, 400)
        );
        
        hostList.add(host);
    }
    
    String arch = "x86";
    String os = "Linux";
    String vmm = "Xen";
    double time_zone = 10.0;
    double costPerCpu = 3.0;
    double costPerMem = 0.05;
    double costPerStorage = 0.1;
    double costPerBw = 0.1;
    LinkedList<Storage> storageList = new LinkedList<>();
    
    DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
        arch, os, vmm, hostList, time_zone,
        costPerCpu, costPerMem, costPerStorage, costPerBw);
    
    PowerDatacenter datacenter = null;
    try {
        datacenter = new PowerDatacenter(name, characteristics, 
            new VmAllocationPolicySimple(hostList), storageList, 
            1); // Changed from 0 to 300 (scheduling interval in seconds)
    } catch (Exception e) {
        e.printStackTrace();
    }
    
    return datacenter;
}


	/**
	 * Prints the Cloudlet objects
	 * @param list  list of Cloudlets
	 */
	private static void printCloudletList(List<Cloudlet> list) {
		Cloudlet cloudlet;

		String indent = "    ";
		Log.println();
		Log.println("========== OUTPUT ==========");
		Log.println("Cloudlet ID" + indent + "STATUS" + indent +
				"Data center ID" + indent + "VM ID" + indent + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
        for (Cloudlet value : list) {
            cloudlet = value;
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
                Log.print("SUCCESS");

                Log.println(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getGuestId() +
                        indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
                        indent + indent + dft.format(cloudlet.getExecStartTime()) + indent + indent + indent + dft.format(cloudlet.getExecFinishTime()));
            }
        }

        
	}
}
