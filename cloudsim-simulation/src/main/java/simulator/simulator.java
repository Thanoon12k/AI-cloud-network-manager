package simulator;

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
import org.cloudsimplus.power.models.PowerModelHostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;
import org.cloudsimplus.distributions.ContinuousDistribution;
import org.cloudsimplus.distributions.UniformDistr;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static simulator.SimulationConfig.*;

public class simulator {

    private final CloudSimPlus simulation;
    private final DatacenterBroker broker;
    private List<Datacenter> datacenterList = new ArrayList<>();
    private final List<Vm> vmList;
    private final List<Cloudlet> cloudletList;

    private final FilesManager filesManager = new FilesManager();
    private final DisplayManager displayManager = new DisplayManager();

    public static void main(String[] args) {
        new simulator();
    }

    public simulator() {
        System.out.println("Initializing CloudSim Plus Environment...");
        simulation = new CloudSimPlus();
        
        datacenterList = createDatacenters(NUM_DATACENTERS);
        broker = new DatacenterBrokerSimple(simulation);
        
        vmList = createVms();
        broker.submitVmList(vmList);

        // Change this to 'false' to trigger the CSV reading logic
        boolean use_fixed_values = false; 
        cloudletList = createCloudlets(use_fixed_values);
        broker.submitCloudletList(cloudletList);

        System.out.printf("Starting simulation with %d Cloudlets (from CSV) on %d VMs...%n",
                cloudletList.size(), vmList.size());
        
        simulation.start();

        List<Cloudlet> finished = broker.getCloudletFinishedList();
        new CloudletsTableBuilder(finished).build();

        displayManager.printFinalReport(simulation, broker, datacenterList, finished);

        String filename = "simu_"+NUM_CLOUDLETS+"_results.txt";
        filesManager.writeCompleteReportToFile(filename, simulation, broker, datacenterList, vmList, finished);
    }

    // ================= CSV DRIVEN FACTORY METHOD =================

    private List<Cloudlet> createCloudlets(boolean use_fixed_values) {
        List<Cloudlet> list = new ArrayList<>();
        UtilizationModelFull fullUtil = new UtilizationModelFull();
        ContinuousDistribution arrivalDist = new UniformDistr(0, 100);

        if (use_fixed_values) {
            // Original logic for fixed values
            for (int i = 0; i < NUM_CLOUDLETS; i++) {
                Cloudlet cloudlet = new CloudletSimple(CLOUDLET_LENGTH, CLOUDLET_PES);
                cloudlet.setFileSize(CLOUDLET_FILE_SIZE).setOutputSize(CLOUDLET_OUTPUT_SIZE);
                cloudlet.setUtilizationModel(fullUtil).setSubmissionDelay(arrivalDist.sample());
                list.add(cloudlet);
            }
        } else {
            // New logic: Read from global_dataset.csv
            String csvFile = "global_dataset.csv";
            String cvsSplitBy = ",";

            try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                br.readLine(); // Skip the header row

                for (int i = 0; i < NUM_CLOUDLETS; i++) {
                    // Split using -1 to handle empty columns if they exist
                    String[] data = br.readLine().split(cvsSplitBy, -1);
                    
                    try {
                    	long tasks_scaler=1;
                        // Mapping based on your provided CSV structure:
                        // Index 1: TotalLength | Index 2: TaskOutputFileSize | Index 3: TaskFileLength
                        long length = Long.parseLong(data[1])*tasks_scaler;
                        long outputSize = Long.parseLong(data[2])*tasks_scaler;
                        long fileSize = Long.parseLong(data[3])*tasks_scaler;

                        Cloudlet cloudlet = new CloudletSimple(length, CLOUDLET_PES);
                        cloudlet.setFileSize(fileSize);
                        cloudlet.setOutputSize(outputSize);
                        
                        // Set standard utilization models
                        cloudlet.setUtilizationModelCpu(fullUtil);
                        cloudlet.setUtilizationModelRam(fullUtil);
                        cloudlet.setUtilizationModelBw(fullUtil).setSubmissionDelay(arrivalDist.sample());

                        list.add(cloudlet);
                    } catch (Exception e) {
                        System.err.println("Skipping invalid row: " + br.readLine());
                    }
                }
            } catch (IOException e) {
                System.err.println("Could not find global_dataset.csv. Ensure it is in the project root.");
            }
        }
        return list;
    }

    // ================= INFRASTRUCTURE METHODS =================

 // ================= INFRASTRUCTURE METHODS =================

 // ================= INFRASTRUCTURE METHODS =================

    /**
     * Creates Datacenters with tiered Host specifications.
     * Each host is scaled to 500MB RAM to ensure 5 tiered VMs (up to 350MB total) 
     * fit while keeping ~30% buffer.
     */
    private List<Datacenter> createDatacenters(int dcs_count) {
        for (int i = 1; i <= dcs_count; i++) {
            List<Host> hostList = new ArrayList<>();
            for (int j = 0; j < HOSTS_PER_DATACENTER; j++) {
                List<Pe> peList = new ArrayList<>();
                
                // MIPS Tiering: Different DCs have different CPU speeds
                // DC 1: 3000 MIPS | DC 2: 3500 MIPS | DC 3: 4000 MIPS
                long hostMips = 2500 + (i * 500); 
                for (int p = 0; p < PES_PER_HOST; p++) {
                    peList.add(new PeSimple(hostMips)); 
                }

                // RESOURCE SCALING: 
                // To fit 5 VMs where some are "Large" (128MB), we need 400-500MB
                long hostRam = 500;     // 500 MB
                long hostBw = 1000;     // 1000 Mbps
                long hostStorage = 5000; // 5000 GB

                HostSimple host = new HostSimple(hostRam, hostBw, hostStorage, peList);
                
                // Power usage scales with the DC index (simulating newer/older hardware)
                double maxPower = 200 + (i * 20);
                double staticPower = 50 + (i * 10);
                host.setPowerModel(new PowerModelHostSimple(maxPower, staticPower));
                
                host.enableUtilizationStats();
                hostList.add(host);
            }
            
            DatacenterSimple dc = new DatacenterSimple(simulation, hostList);
            
            // Tiered Pricing: DC 3 is significantly more expensive than DC 1
            dc.getCharacteristics()
              .setCostPerSecond(1.0 + (i * 1.5)/50)
              .setCostPerMem(0.5 + (i * 0.5)/50)
              .setCostPerStorage(0.1 + (i * 0.2)/50)
              .setCostPerBw(0.05 + (i * 0.1)/50);
            
            datacenterList.add(dc);
        }
        return datacenterList;
    }

    /**
     * Creates 15 VMs using a T-Shirt Size (S, M, L) Tiering System.
     */
    private List<Vm> createVms() {
        List<Vm> list = new ArrayList<>();
        for (int i = 1; i <= NUM_VMS; i++) {
            long mipsTier;
            int ramTier;
            String tierName;

            // Tiering Logic: 1=Small, 2=Medium, 3=Large, 4=Small...
            if (i % 3 == 0) {
                tierName = "LARGE";
                mipsTier = 1500;
                ramTier = 128;
            } else if (i % 2 == 0) {
                tierName = "MEDIUM";
                mipsTier = 800;
                ramTier = 64;
            } else {
                tierName = "SMALL";
                mipsTier = 400;
                ramTier = 32;
            }

            Vm vm = new VmSimple(i, mipsTier, VM_PES);
            vm.setRam(ramTier)
              .setBw(20 + (i * 5))  // Tiered Bandwidth
              .setSize(200 + (i * 10)); // Tiered Storage
            
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            vm.enableUtilizationStats();
            
            // Note: CloudSim Plus provides Description, good for tracking tiers in logs
            vm.setDescription(tierName); 
            
            list.add(vm);
        }
        return list;
    }
}