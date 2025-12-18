package simulator;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmCost;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles writing all simulation reports to a single file.
 * FIXED: Uses a single PrintWriter stream to avoid file-locking IOExceptions.
 */
public class FilesManager {

    public void writeCompleteReportToFile(
            String filename,
            CloudSimPlus simulation,
            DatacenterBroker broker,
            List<Datacenter> datacenterList,
            List<Vm> vmList,
            List<Cloudlet> finishedList
    ) {
        // Use a single try-with-resources for the entire file stream
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("================================================================================");
            writer.println("                  CLOUDSIM PLUS SIMULATION COMPREHENSIVE REPORT");
            writer.println("================================================================================");
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println();

            // 1. Static Configuration Summary
            writeConfigurationSummary(writer, datacenterList, vmList, finishedList);
            
            // 2. Performance Metrics
            writeTimingAnalysisToFile(writer, finishedList, simulation);
            
            // 3. Financial Metrics
            writeCostAnalysisToFile(writer, finishedList, broker, simulation);
            
            // 4. Energy Metrics
            writePowerAnalysisToFile(writer, datacenterList, simulation);
            
            // 5. Resource Efficiency
            writeUtilizationAnalysisToFile(writer, finishedList, datacenterList, broker);
            
            // 6. Distribution & Network
            writeNetworkDistributionToFile(writer, finishedList, broker, datacenterList);
            
            // 7. Physical Infrastructure (Merged into same stream)
            writeInfrastructureSpecsToStream(writer, datacenterList, vmList);

            System.out.println("Full report successfully saved to: " + filename);
        } catch (IOException e) {
            System.err.println("Critical Error writing report file: " + e.getMessage());
        }
    }

    private void writeConfigurationSummary(PrintWriter writer, List<Datacenter> dcs, List<Vm> vms, List<Cloudlet> cloudlets) {
        writer.println("├─ SIMULATION SCOPE");
        writer.println("│");
        writer.printf("│  Datacenters    : %d%n", dcs.size());
        writer.printf("│  Hosts (Total)  : %d%n", dcs.stream().mapToInt(dc -> dc.getHostList().size()).sum());
        writer.printf("│  Virtual Machines: %d%n", vms.size());
        writer.printf("│  Workload Tasks : %d Cloudlets%n", cloudlets.size());
        writer.println("│");
        writer.println("└─────────────────────────────────────────────────────────────────────");
        writer.println();
    }

    private void writeTimingAnalysisToFile(PrintWriter writer, List<Cloudlet> list, CloudSimPlus simulation) {
        writer.println("├─ 1. TIMING & QoS ANALYSIS");
        writer.println("│");

        double totalWait = 0, totalExec = 0;
        int count = list.size();

        for (Cloudlet c : list) {
            totalWait += (c.getStartTime() - c.getSubmissionDelay());
            totalExec += (c.getFinishTime() - c.getStartTime());
        }

        writer.printf("│  Simulation Time : %.2f seconds%n", simulation.clock());
        writer.printf("│  Avg Execution   : %.4f sec%n", count > 0 ? totalExec / count : 0);
        writer.printf("│  Avg Queue Wait  : %.4f sec%n", count > 0 ? totalWait / count : 0);
        writer.println("└─────────────────────────────────────────────────────────────────────");
        writer.println();
    }

    private void writeCostAnalysisToFile(PrintWriter writer, List<Cloudlet> finishedList, DatacenterBroker broker, CloudSimPlus simulation) {
        writer.println("├─ 2. COST ANALYSIS (USD)");
        writer.println("│");

        double total = 0, cpu = 0, mem = 0, storage = 0;
        List<Vm> createdVms = broker.getVmCreatedList();

        for (Vm vm : createdVms) {
            VmCost vc = new VmCost(vm);
            total += vc.getTotalCost();
            cpu += vc.getProcessingCost();
            mem += vc.getMemoryCost();
            storage += vc.getStorageCost();
        }

        writer.printf("│  Infrastructure Total : $%.6f%n", total);
        writer.printf("│  CPU Component        : $%.6f%n", cpu);
        writer.printf("│  Memory Component     : $%.6f%n", mem);
        writer.printf("│  Avg Cost per Task    : $%.6f%n", finishedList.isEmpty() ? 0 : total / finishedList.size());
        writer.println("└─────────────────────────────────────────────────────────────────────");
        writer.println();
    }

    private void writePowerAnalysisToFile(PrintWriter writer, List<Datacenter> dcs, CloudSimPlus simulation) {
        writer.println("├─ 3. POWER & ENERGY ANALYSIS");
        writer.println("│");

        double totalP = 0, totalStatic = 0;
        int hosts = 0;

        for (Datacenter dc : dcs) {
            for (Host host : dc.getHostList()) {
                double util = host.getCpuUtilizationStats().getMean();
                totalP += host.getPowerModel().getPower(util);
                totalStatic += host.getPowerModel().getTotalShutDownPower(); // Use model value, not constant
                hosts++;
            }
        }

        double energyJ = totalP * simulation.clock();
        writer.printf("│  Active Power Consumption : %.2f Watts%n", totalP);
        writer.printf("│  Static Power (Waste)     : %.2f Watts%n", totalStatic);
        writer.printf("│  Total Energy (Joules)    : %.2f J%n", energyJ);
        writer.printf("│  Carbon Footprint (est)   : %.4f kg CO2%n", (energyJ / 3600000) * 0.5);
        writer.println("└─────────────────────────────────────────────────────────────────────");
        writer.println();
    }

    private void writeUtilizationAnalysisToFile(PrintWriter writer, List<Cloudlet> finishedList, List<Datacenter> datacenterList, DatacenterBroker broker) {
        writer.println("├─ 4. RESOURCE UTILIZATION");
        writer.println("│");

        long hostRam = 0, vmRam = 0;
        for (Datacenter dc : datacenterList) 
            for (Host h : dc.getHostList()) hostRam += h.getRam().getCapacity();
        for (Vm vm : broker.getVmCreatedList()) vmRam += vm.getRam().getCapacity();

        writer.printf("│  RAM Allocation Efficiency : %.2f%%%n", hostRam > 0 ? (double)vmRam/hostRam*100 : 0);
        writer.printf("│  Unused Host Memory        : %d MB%n", hostRam - vmRam);
        writer.println("└─────────────────────────────────────────────────────────────────────");
        writer.println();
    }

    private void writeNetworkDistributionToFile(PrintWriter writer, List<Cloudlet> finished, DatacenterBroker broker, List<Datacenter> dcs) {
        writer.println("├─ 5. TOPOLOGY & DATA FLOW");
        writer.println("│");
        writer.println("│  Star Topology Distribution:");

        for (Datacenter dc : dcs) {
            long count = finished.stream()
                .filter(c -> c.getVm() != null && c.getVm().getHost().getDatacenter().equals(dc))
                .count();
            writer.printf("│    DC %d Mapping: %d Tasks (%.1f%%)%n", dc.getId(), count, finished.isEmpty() ? 0 : (double)count/finished.size()*100);
        }
        writer.println("└─────────────────────────────────────────────────────────────────────");
    }

    // FIXED: Now writes directly to the PW stream instead of re-opening the file
    private void writeInfrastructureSpecsToStream(PrintWriter pw, List<Datacenter> dcs, List<Vm> vms) {
        pw.println("\n" + "=".repeat(80));
        pw.println("                 DETAILED NETWORK ELEMENT SPECIFICATIONS");
        pw.println("=".repeat(80));

        pw.println("\n[1] DATACENTERS AND HOSTS");
        for (Datacenter dc : dcs) {
            var ch = dc.getCharacteristics();
            pw.printf("DC %d | Host Specs: RAM: %dMB | MIPS/Core: %.0f | Cost/Sec: %.3f%n",
                    dc.getId(), dc.getHostList().get(0).getRam().getCapacity(),
                    dc.getHostList().get(0).getMips(), ch.getCostPerSecond());
        }

        pw.println("\n[2] VIRTUAL MACHINES (TIERED)");
        pw.printf("%-4s | %-8s | %-6s | %-8s | %-8s%n", "ID", "Tier", "MIPS", "RAM(MB)", "Scheduler");
        pw.println("-".repeat(50));
        for (Vm vm : vms) {
            pw.printf("%-4d | %-8s | %-6.0f | %-8d | %s%n",
                    vm.getId(), vm.getDescription(), vm.getMips(), 
                    vm.getRam().getCapacity(), vm.getCloudletScheduler().getClass().getSimpleName());
        }

        pw.println("\n" + "=".repeat(80));
        pw.println("                        END OF SIMULATION REPORT");
        pw.println("=".repeat(80));
    }
}