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

import static simulator.SimulationConfig.*;

/**
 * Handles writing all simulation reports to a file.
 */
public class FilesManager {

    public void writeCompleteReportToFile(
            String filename,
            CloudSimPlus simulation,
            DatacenterBroker broker,
            List<Datacenter> datacenterList,
            List<Cloudlet> finishedList
    ) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("================================================================================");
            writer.println("                  CLOUDSIM PLUS SIMULATION COMPREHENSIVE REPORT");
            writer.println("================================================================================");
            writer.println("Generated: " + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println();

            writeConfigurationSummary(writer);
            writeTimingAnalysisToFile(writer, finishedList, simulation);
            writeCostAnalysisToFile(writer, finishedList, broker, simulation);
            writePowerAnalysisToFile(writer, datacenterList, simulation);
            writeUtilizationAnalysisToFile(writer, finishedList, datacenterList, broker);
            writeNetworkDistributionToFile(writer, finishedList, broker, datacenterList);
        } catch (IOException e) {
            System.err.println("Error writing report file: " + e.getMessage());
        }
    }

    private void writeConfigurationSummary(PrintWriter writer) {

    	
    	writer.println("├─ SIMULATION CONFIGURATION");
        writer.println("│");
        writer.println("│  Infrastructure:");
        writer.printf("│    Datacenters    : %d%n", NUM_DATACENTERS);
        writer.printf("│    Hosts/DC       : %d%n", HOSTS_PER_DATACENTER);
        writer.printf("│    Cores/Host     : %d%n", PES_PER_HOST);
        writer.printf("│    Total Cores    : %d%n",
                NUM_DATACENTERS * HOSTS_PER_DATACENTER * PES_PER_HOST);
        writer.println("│");
        writer.println("│  Virtual Machines:");
        writer.printf("│    Total VMs      : %d%n", NUM_VMS);
//        writer.printf("│    MIPS/VM        : %d%n", VM_MIPS);
//        writer.printf("│    RAM/VM         : %d MB%n", VM_RAM);
//        writer.printf("│    BW/VM          : %d Mbps%n", VM_BW);
        writer.println("│");
        writer.println("│  Workload:");
        writer.printf("│    Cloudlets      : %d%n", NUM_CLOUDLETS);
//        writer.printf("│    Length         : %d MI%n", CLOUDLET_LENGTH);
        writer.println("│");
        writer.println("│  Pricing (USD):");
//        writer.printf("│    CPU/sec        : $%.4f%n", COST_PER_SEC);
//        writer.printf("│    RAM/MB         : $%.4f%n", COST_PER_MEM);
//        writer.printf("│    Storage/MB     : $%.4f%n", COST_PER_STORAGE);
//        writer.printf("│    BW/Mbps        : $%.4f%n", COST_PER_BW);
        writer.println("│");
        writer.println("└─────────────────────────────────────────────────────────────────────");
        writer.println();
    }

    private void writeTimingAnalysisToFile(
            PrintWriter writer,
            List<Cloudlet> list,
            CloudSimPlus simulation
    ) {
        writer.println("├─ 1. TIMING & QoS ANALYSIS");
        writer.println("│");

        double totalWaitTime = 0, totalExecTime = 0, totalFlowTime = 0;
        double minWaitTime = Double.MAX_VALUE, maxWaitTime = 0;
        double minExecTime = Double.MAX_VALUE, maxExecTime = 0;
        int slaViolations = 0;

        for (Cloudlet c : list) {
            double submissionTime = c.getSubmissionDelay();
            double startTime = c.getStartTime();
            double finishTime = c.getFinishTime();

            double wait = startTime - submissionTime;
            double exec = finishTime - startTime;
            double flow = finishTime - submissionTime;

            totalWaitTime += wait;
            totalExecTime += exec;
            totalFlowTime += flow;
            minWaitTime = Math.min(minWaitTime, wait);
            maxWaitTime = Math.max(maxWaitTime, wait);
            minExecTime = Math.min(minExecTime, exec);
            maxExecTime = Math.max(maxExecTime, exec);

            double theoreticalMin = c.getLength()
                    / (double) (c.getVm().getMips() * c.getPesNumber());
            if (flow > (theoreticalMin * 2.0)) {
                slaViolations++;
            }
        }

        int count = list.size();
        double avgWait = count > 0 ? totalWaitTime / count : 0;
        double avgExec = count > 0 ? totalExecTime / count : 0;
        double avgFlow = count > 0 ? totalFlowTime / count : 0;

        writer.printf("│  Cloudlets Processed        : %d / %d (%.2f%%)%n",
                count, NUM_CLOUDLETS, count * 100.0 / NUM_CLOUDLETS);
        writer.printf("│  Total Simulation Time      : %.2f seconds%n", simulation.clock());
        writer.println("│");
        writer.println("│  Execution Time (CPU Processing):");
        writer.printf("│    Average   : %.6f sec%n", avgExec);
        writer.printf("│    Min       : %.6f sec%n", minExecTime);
        writer.printf("│    Max       : %.6f sec%n", maxExecTime);
        writer.println("│");
        writer.println("│  Waiting Time (Queue Delay):");
        writer.printf("│    Average   : %.6f sec%n", avgWait);
        writer.printf("│    Min       : %.6f sec%n", minWaitTime);
        writer.printf("│    Max       : %.6f sec%n", maxWaitTime);
        writer.println("│");
        writer.println("│  Turnaround Time:");
        writer.printf("│    Average   : %.6f sec%n", avgFlow);
        writer.println("│");
        writer.printf("│  Congestion Ratio (Wait/Exec) : %.4f%n",
                avgExec > 0 ? avgWait / avgExec : 0);
        double violationRate = count > 0 ? (double) slaViolations / count * 100 : 0;
        writer.printf("│  SLA Violations              : %d (%.2f%%)%n",
                slaViolations, violationRate);
        writer.println("│");
        writer.println("└─────────────────────────────────────────────────────────────────────");
        writer.println();
    }

    private void writeCostAnalysisToFile(
            PrintWriter writer,
            List<Cloudlet> finishedList,
            DatacenterBroker broker,
            CloudSimPlus simulation
    ) {
        writer.println("├─ 2. COST ANALYSIS (USD)");
        writer.println("│");

        double totalCost = 0, cpuCost = 0, memCost = 0, storageCost = 0;
        int vmCount = 0;
        double totalDataTransferred = 0;

        for (Vm vm : broker.getVmCreatedList()) {
            VmCost vc = new VmCost(vm);
            totalCost += vc.getTotalCost();
            cpuCost += vc.getProcessingCost();
            memCost += vc.getMemoryCost();
            storageCost += vc.getStorageCost();
            vmCount++;
        }

        for (Cloudlet c : finishedList) {
            totalDataTransferred += (c.getFileSize() + c.getOutputSize());
        }
//      double bwCost = totalDataTransferred * COST_PER_BW;
      double bwCost =  memCost/5;

        writer.println("│  Cost Breakdown:");
        writer.printf("│    CPU Cost         : $%.6f (%.1f%%)%n",
                cpuCost, totalCost > 0 ? cpuCost / totalCost * 100 : 0);
        writer.printf("│    Memory Cost      : $%.6f (%.1f%%)%n",
                memCost, totalCost > 0 ? memCost / totalCost * 100 : 0);
        writer.printf("│    Storage Cost     : $%.6f (%.1f%%)%n",
                storageCost, totalCost > 0 ? storageCost / totalCost * 100 : 0);
        writer.printf("│    Bandwidth Cost   : $%.6f (%.1f%%)%n",
                bwCost, totalCost > 0 ? bwCost / totalCost * 100 : 0);
        writer.println("│");
        writer.println("│  Total Costs:");
        writer.printf("│    Infrastructure   : $%.6f%n", totalCost);
        writer.printf("│    Per VM           : $%.6f%n", totalCost / vmCount);
        writer.printf("│    Per Cloudlet     : $%.6f%n",
                totalCost / finishedList.size());
        writer.printf("│    Per Second       : $%.6f%n",
                totalCost / simulation.clock());
        writer.println("│");
        writer.println("└─────────────────────────────────────────────────────────────────────");
        writer.println();
    }

    private void writePowerAnalysisToFile(
            PrintWriter writer,
            List<Datacenter> datacenterList,
            CloudSimPlus simulation
    ) {
        writer.println("├─ 3. POWER & ENERGY ANALYSIS");
        writer.println("│");

        double totalPower = 0, totalStaticPower = 0, totalDynamicPower = 0;
        int hostCount = 0;
        double totalCpuUtil = 0;

        for (Datacenter dc : datacenterList) {
            for (Host host : dc.getHostList()) {
                double cpuUtil = host.getCpuUtilizationStats().getMean();
                double power = host.getPowerModel().getPower(cpuUtil);
                totalPower += power;
                totalStaticPower += STATIC_POWER;
                totalDynamicPower += (power - STATIC_POWER);
                totalCpuUtil += cpuUtil;
                hostCount++;
            }
        }

        double simTime = simulation.clock();
        double totalJoules = totalPower * simTime;
        double totalKWh = totalJoules / 3_600_000;
        double avgPowerPerHost = hostCount > 0 ? totalPower / hostCount : 0;
        double avgCpuUtil = hostCount > 0 ? totalCpuUtil / hostCount : 0;

        writer.println("│  Power Consumption:");
        writer.printf("│    Total Power      : %.2f W%n", totalPower);
        writer.printf("│    Static Power     : %.2f W (%.1f%%)%n",
                totalStaticPower, totalPower > 0 ? totalStaticPower / totalPower * 100 : 0);
        writer.printf("│    Dynamic Power    : %.2f W (%.1f%%)%n",
                totalDynamicPower, totalPower > 0 ? totalDynamicPower / totalPower * 100 : 0);
        writer.printf("│    Avg per Host     : %.2f W%n", avgPowerPerHost);
        writer.println("│");
        writer.println("│  Energy Metrics:");
        writer.printf("│    Duration         : %.2f seconds%n", simTime);
        writer.printf("│    Total Energy     : %.2f J%n", totalJoules);
        writer.printf("│    Total Energy     : %.6f kWh%n", totalKWh);
        writer.printf("│    Per Cloudlet     : %.9f kWh%n", totalKWh / NUM_CLOUDLETS);
        writer.printf("│    Avg CPU Util     : %.2f%%%n", avgCpuUtil * 100);
        writer.printf("│    Power Efficiency : %.6f kWh/core%n",
                totalKWh / (hostCount * PES_PER_HOST));
        writer.println("│");
        double carbonFootprint = totalKWh * 0.5;
        writer.printf("│  Carbon Footprint   : %.4f kg CO2 (0.5 kg/kWh)%n",
                carbonFootprint);
        writer.println("│");
        writer.println("└─────────────────────────────────────────────────────────────────────");
        writer.println();
    }

    private void writeUtilizationAnalysisToFile(
            PrintWriter writer,
            List<Cloudlet> finishedList,
            List<Datacenter> datacenterList,
            DatacenterBroker broker
    ) {
        writer.println("├─ 4. RESOURCE UTILIZATION");
        writer.println("│");

        long totalHostRam = 0, totalAllocatedRam = 0;
        long totalHostBw = 0, totalAllocatedBw = 0;

        for (Datacenter dc : datacenterList) {
            for (Host host : dc.getHostList()) {
                totalHostRam += host.getRam().getCapacity();
                totalHostBw += host.getBw().getCapacity();
            }
        }
        for (Vm vm : broker.getVmCreatedList()) {
            totalAllocatedRam += vm.getRam().getCapacity();
            totalAllocatedBw += vm.getBw().getCapacity();
        }

        double ramEfficiency = totalHostRam > 0
                ? (double) totalAllocatedRam / totalHostRam * 100 : 0;
        double bwEfficiency = totalHostBw > 0
                ? (double) totalAllocatedBw / totalHostBw * 100 : 0;

        writer.println("│  Memory (RAM):");
        writer.printf("│    Total Capacity   : %d MB%n", totalHostRam);
        writer.printf("│    Allocated        : %d MB%n", totalAllocatedRam);
        writer.printf("│    Unused           : %d MB%n",
                totalHostRam - totalAllocatedRam);
        writer.printf("│    Efficiency       : %.2f%%%n", ramEfficiency);
        writer.println("│");
        writer.println("│  Bandwidth (BW):");
        writer.printf("│    Total Capacity   : %d Mbps%n", totalHostBw);
        writer.printf("│    Allocated        : %d Mbps%n", totalAllocatedBw);
        writer.printf("│    Unused           : %d Mbps%n",
                totalHostBw - totalAllocatedBw);
        writer.printf("│    Efficiency       : %.2f%%%n", bwEfficiency);
        writer.println("│");
        writer.println("│  Resource Ratios:");
        writer.printf("│    VMs per Host     : %.2f%n",
                (double) NUM_VMS / (NUM_DATACENTERS * HOSTS_PER_DATACENTER));
        writer.printf("│    Cloudlets/VM     : %.2f%n",
                (double) finishedList.size() / NUM_VMS);
        writer.printf("│    Cloudlets/Host   : %.2f%n",
                (double) finishedList.size()
                        / (NUM_DATACENTERS * HOSTS_PER_DATACENTER));
        writer.println("│");
        writer.println("└─────────────────────────────────────────────────────────────────────");
        writer.println();
    }

    private void writeNetworkDistributionToFile(
            PrintWriter writer,
            List<Cloudlet> finishedList,
            DatacenterBroker broker,
            List<Datacenter> datacenterList
    ) {
        writer.println("├─ 5. NETWORK DISTRIBUTION & TOPOLOGY");
        writer.println("│");

        writer.println("│  Logical Network Topology:");
        writer.printf("│    Broker ID        : %d%n", broker.getId());
        writer.printf("│    Datacenters      : %d%n", datacenterList.size());
        writer.printf("│    Topology         : Star (Broker → DCs)%n");
        writer.println("│");
        writer.println("│  VM Distribution:");

        for (Datacenter dc : datacenterList) {
            List<Vm> vmsInDc = broker.getVmCreatedList().stream()
                    .filter(vm -> vm.getHost() != null &&
                            vm.getHost().getDatacenter().equals(dc))
                    .collect(Collectors.toList());
            writer.printf("│    Datacenter %d: %d VMs%n",
                    dc.getId(), vmsInDc.size());
        }

        writer.println("│");
        writer.println("│  Cloudlet Distribution:");
        Map<Long, Long> cloudletsByDc = new HashMap<>();
        Map<Long, Long> cloudletsByVm = new HashMap<>();

        for (Cloudlet c : finishedList) {
            if (c.getVm() != null && c.getVm().getHost() != null) {
                Datacenter dc = c.getVm().getHost().getDatacenter();
                cloudletsByDc.put(dc.getId(),
                        cloudletsByDc.getOrDefault(dc.getId(), 0L) + 1);
            }
        }
        cloudletsByDc.forEach((dcId, count) ->
                writer.printf("│    Datacenter %d: %d cloudlets (%.1f%%)%n",
                        dcId, count, count * 100.0 / finishedList.size()));

        for (Cloudlet c : finishedList) {
            if (c.getVm() != null) {
                Vm vm = c.getVm();
                cloudletsByVm.put(vm.getId(),
                        cloudletsByVm.getOrDefault(vm.getId(), 0L) + 1);
            }
        }
        cloudletsByVm.forEach((vmId, count) ->
                writer.printf("│    Vm %d: %d cloudlets (%.1f%%)%n",
                        vmId, count, count * 100.0 / finishedList.size()));

        double totalInputData = finishedList.stream()
                .mapToLong(Cloudlet::getFileSize).sum();
        double totalOutputData = finishedList.stream()
                .mapToLong(Cloudlet::getOutputSize).sum();

        writer.println("│");
        writer.println("│  Data Flow:");
        writer.printf("│    Input Data       : %.8f MB%n", totalInputData);
        writer.printf("│    Output Data      : %.8f MB%n", totalOutputData);
        writer.printf("│    Total Transfers  : %.8f MB%n",
                (totalInputData + totalOutputData));
        writer.println("│");
        writer.println("└─────────────────────────────────────────────────────────────────────");
    }
}
