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
        filesManager.writeCompleteReportToFile(filename, simulation, broker, datacenterList, finished);
    }

    // ================= CSV DRIVEN FACTORY METHOD =================

    private List<Cloudlet> createCloudlets(boolean use_fixed_values) {
        List<Cloudlet> list = new ArrayList<>();
        UtilizationModelFull fullUtil = new UtilizationModelFull();

        if (use_fixed_values) {
            // Original logic for fixed values
            ContinuousDistribution arrivalDist = new UniformDistr(0, 1000);
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
                        cloudlet.setUtilizationModelBw(fullUtil);

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

    private List<Datacenter> createDatacenters(int dcs_count) {
        for (int i = 1; i <= dcs_count; i++) {
            List<Host> hostList = new ArrayList<>();
            for (int j = 0; j < HOSTS_PER_DATACENTER; j++) {
                List<Pe> peList = new ArrayList<>();
                // Increase MIPS to handle the cumulative MIPS of 5 VMs
                for (int p = 0; p < PES_PER_HOST; p++) {
                    peList.add(new PeSimple(5000)); 
                }

                // RAM: (avg 32MB * 5 VMs) / 0.8 = 200MB
                // BW:  (avg 24Mbps * 5 VMs) / 0.8 = 150Mbps
                // Storage: (avg 116GB * 5 VMs) / 0.8 = 725GB
                long hostRam = 250; 
                long hostBw = 200;
                long hostStorage = 1000;

                HostSimple host = new HostSimple(hostRam, hostBw, hostStorage, peList);
                host.setPowerModel(new PowerModelHostSimple(MAX_POWER, STATIC_POWER));
                host.enableUtilizationStats();
                hostList.add(host);
            }
            DatacenterSimple dc = new DatacenterSimple(simulation, hostList);
            dc.getCharacteristics()
              .setCostPerSecond( 1 + (i * 2)).setCostPerMem( 1 + (i * 2))
              .setCostPerStorage( 1 + (i * 2)).setCostPerBw( 0.5 + (i * 2));
            datacenterList.add(dc);
        }
        return datacenterList;
    }

    private List<Vm> createVms() {
        List<Vm> list = new ArrayList<>();
        // Note: Using VM_PES from config to ensure consistency
        for (int i = 1; i <= NUM_VMS; i++) {
            Vm vm = new VmSimple(i, 3 + (i * 2), VM_PES);
            vm.setRam(16 + (i * 2))
              .setBw(8 + (i * 2))
              .setSize(100 + (i * 2));
            
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            vm.enableUtilizationStats();
            list.add(vm);
        }
        return list;
    }
}