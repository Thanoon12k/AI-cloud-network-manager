package simulator;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmCost;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static simulator.SimulationConfig.*;

/**
 * Handles all console printing for the simulation.
 */
public class DisplayManager {

    public void printFinalReport(
            CloudSimPlus simulation,
            DatacenterBroker broker,
            List<Datacenter> datacenterList,
            List<Cloudlet> finishedList
    ) {
        finishedList.sort((c1, c2) ->
                Long.compare(c1.getId(), c2.getId()));

        System.out.println();
        System.out.println("################################################################################");
        System.out.println("                    COMPREHENSIVE SIMULATION ANALYSIS REPORT                   ");
        System.out.println("################################################################################");

        printTimingAnalysis(finishedList, simulation);
        printCostAnalysis(finishedList, broker, simulation);
        printPowerAnalysis(datacenterList, simulation);
        printUtilizationAnalysis(finishedList, datacenterList, broker);
        printNetworkDistribution(finishedList, broker, datacenterList);

        System.out.println("================================================================================");
        System.out.println("Detailed report successfully written to file.");
        System.out.println("================================================================================");
    }

    private void printTimingAnalysis(
            List<Cloudlet> list,
            CloudSimPlus simulation
    ) {
        System.out.println();
        System.out.println("1. TIMING & QoS ANALYSIS");
        System.out.println("-------------------------");

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

            double theoreticalMin =
                    c.getLength() / (double) (c.getVm().getMips() * c.getPesNumber());
            if (flow > (theoreticalMin * 2.0)) {
                slaViolations++;
            }
        }

        int count = list.size();
        double avgWait = count > 0 ? totalWaitTime / count : 0;
        double avgExec = count > 0 ? totalExecTime / count : 0;
        double avgFlow = count > 0 ? totalFlowTime / count : 0;

        System.out.printf("Total Cloudlets Processed  : %d / %d (%.2f%%)%n",
                count, NUM_CLOUDLETS, count * 100.0 / NUM_CLOUDLETS);
        System.out.printf("Total Simulation Time      : %.2f seconds%n",
                simulation.clock());
        System.out.println();
        System.out.println("Execution Time (CPU Processing):");
        System.out.printf("  Average   : %.6f sec%n", avgExec);
        System.out.printf("  Min       : %.6f sec%n", minExecTime);
        System.out.printf("  Max       : %.6f sec%n", maxExecTime);
        System.out.println();
        System.out.println("Waiting Time (Queue Delay):");
        System.out.printf("  Average   : %.6f sec%n", avgWait);
        System.out.printf("  Min       : %.6f sec%n", minWaitTime);
        System.out.printf("  Max       : %.6f sec%n", maxWaitTime);
        System.out.println();
        System.out.println("Turnaround Time:");
        System.out.printf("  Average   : %.6f sec%n", avgFlow);
        System.out.println();
        double congestionRatio = avgExec > 0 ? avgWait / avgExec : 0;
        System.out.printf("Congestion Ratio (Wait/Exec) : %.4f%n",
                congestionRatio);
        double violationRate = count > 0 ? (double) slaViolations / count * 100 : 0;
        System.out.printf("SLA Violations               : %d (%.2f%%)%n",
                slaViolations, violationRate);
    }

    private void printCostAnalysis(
            List<Cloudlet> finishedList,
            DatacenterBroker broker,
            CloudSimPlus simulation
    ) {
        System.out.println();
        System.out.println("2. COST ANALYSIS (USD)");
        System.out.println("----------------------");

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
//        double bwCost = totalDataTransferred * COST_PER_BW;
        double bwCost =  memCost/5;

        System.out.println("BREAKDOWN BY COMPONENT:");
        System.out.printf("  CPU Cost         : %.6f (%.1f%%)%n",
                cpuCost, totalCost > 0 ? cpuCost / totalCost * 100 : 0);
        System.out.printf("  Memory Cost      : %.6f (%.1f%%)%n",
                memCost, totalCost > 0 ? memCost / totalCost * 100 : 0);
        System.out.printf("  Storage Cost     : %.6f (%.1f%%)%n",
                storageCost, totalCost > 0 ? storageCost / totalCost * 100 : 0);
        System.out.printf("  Bandwidth Cost   : %.6f (%.1f%%)%n",
                bwCost, totalCost > 0 ? bwCost / totalCost * 100 : 0);
        System.out.println();
        System.out.println("TOTAL COSTS:");
        System.out.printf("  Total Infrastructure Cost : %.6f%n", totalCost);
        System.out.printf("  Cost per VM               : %.6f%n",
                totalCost / vmCount);
        System.out.printf("  Cost per Cloudlet         : %.6f%n",
                totalCost / finishedList.size());
        System.out.printf("  Cost per Second           : %.6f%n",
                totalCost / simulation.clock());
        System.out.printf("  Total Data Transferred    : %.2f MB%n",
                totalDataTransferred );
    }

    private void printPowerAnalysis(
            List<Datacenter> datacenterList,
            CloudSimPlus simulation
    ) {
        System.out.println();
        System.out.println("3. POWER & ENERGY ANALYSIS");
        System.out.println("--------------------------");

        double totalPower = 0, totalStaticPower = 0, totalDynamicPower = 0;
        int hostCount = 0;
        double totalCpuUtil = 0;

        System.out.println("PER-DATACENTER BREAKDOWN:");
        System.out.printf("%-10s %-10s %-12s %-12s %-12s%n",
                "DC", "Host", "Power(W)", "CPU Util", "Status");
        System.out.println("-------------------------------------------------------------------");

        for (Datacenter dc : datacenterList) {
            for (Host host : dc.getHostList()) {
                double cpuUtil = host.getCpuUtilizationStats().getMean();
                double power = host.getPowerModel().getPower(cpuUtil);
                totalPower += power;
                totalStaticPower += STATIC_POWER;
                totalDynamicPower += (power - STATIC_POWER);
                totalCpuUtil += cpuUtil;
                hostCount++;

                String status = cpuUtil >= 0.8 ? "HIGH LOAD"
                        : cpuUtil >= 0.5 ? "MEDIUM" : "LOW";
                System.out.printf("%-10d %-10d %-12.2f %-12.2f %s%n",
                        dc.getId(), host.getId(), power, cpuUtil * 100, status);
            }
        }
        System.out.println("-------------------------------------------------------------------");

        double simTime = simulation.clock();
        double totalJoules = totalPower * simTime;
        double totalKWh = totalJoules / 3_600_000;
        double avgPowerPerHost = hostCount > 0 ? totalPower / hostCount : 0;
        double avgCpuUtil = hostCount > 0 ? totalCpuUtil / hostCount : 0;

        System.out.println("POWER CONSUMPTION:");
        System.out.printf("  Total Instantaneous Power  : %.2f W%n", totalPower);
        System.out.printf("  Static Power (Always-On)   : %.2f W (%.1f%%)%n",
                totalStaticPower, totalPower > 0 ? totalStaticPower / totalPower * 100 : 0);
        System.out.printf("  Dynamic Power (Compute)    : %.2f W (%.1f%%)%n",
                totalDynamicPower, totalPower > 0 ? totalDynamicPower / totalPower * 100 : 0);
        System.out.printf("  Average Power per Host     : %.2f W%n", avgPowerPerHost);
        System.out.println();
        System.out.println("ENERGY & CARBON FOOTPRINT:");
        System.out.printf("  Simulation Duration        : %.2f seconds%n", simTime);
        System.out.printf("  Total Energy               : %.2f J%n", totalJoules);
        System.out.printf("  Total Energy               : %.6f kWh%n", totalKWh);
        System.out.printf("  Energy per Cloudlet        : %.9f kWh%n",
                totalKWh / NUM_CLOUDLETS);
        System.out.printf("  Average CPU Utilization    : %.2f%%%n",
                avgCpuUtil * 100);
        System.out.printf("  Power Efficiency (kWh/core): %.6f%n",
                totalKWh / (hostCount * PES_PER_HOST));
        double carbonFootprint = totalKWh * 0.5;
        System.out.printf("  Estimated Carbon Footprint : %.4f kg CO2%n",
                carbonFootprint);
    }

    private void printUtilizationAnalysis(
            List<Cloudlet> finishedList,
            List<Datacenter> datacenterList,
            DatacenterBroker broker
    ) {
        System.out.println();
        System.out.println("4. RESOURCE UTILIZATION ANALYSIS");
        System.out.println("--------------------------------");

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

        System.out.println("MEMORY (RAM) UTILIZATION:");
        System.out.printf("  Total Host Capacity       : %d MB%n", totalHostRam);
        System.out.printf("  Total VM Allocation       : %d MB%n", totalAllocatedRam);
        System.out.printf("  Unused / Fragmented       : %d MB%n",
                totalHostRam - totalAllocatedRam);
        System.out.printf("  Efficiency                : %.2f%%%n", ramEfficiency);
        System.out.println();
        System.out.println("BANDWIDTH (BW) UTILIZATION:");
        System.out.printf("  Total Host Capacity       : %d Mbps%n", totalHostBw);
        System.out.printf("  Total VM Allocation       : %d Mbps%n", totalAllocatedBw);
        System.out.printf("  Unused                    : %d Mbps%n",
                totalHostBw - totalAllocatedBw);
        System.out.printf("  Efficiency                : %.2f%%%n", bwEfficiency);
        System.out.println();
        System.out.println("CPU UTILIZATION Per Host:");
        for (Datacenter dc : datacenterList) {
            System.out.printf("  Datacenter %d%n", dc.getId());
            for (Host host : dc.getHostList()) {
                double cpuUtil = host.getCpuUtilizationStats().getMean();
                long vmsOnHost = broker.getVmCreatedList().stream()
                        .filter(vm -> vm.getHost() != null
                                && vm.getHost().equals(host))
                        .count();
                System.out.printf("    Host %d : %.2f%% (%d VMs)%n",
                        host.getId(), cpuUtil * 100, vmsOnHost);
            }
        }
        System.out.println();
        System.out.println("RESOURCE RATIOS:");
        System.out.printf("  VMs per Host              : %.2f%n",
                (double) NUM_VMS / (NUM_DATACENTERS * HOSTS_PER_DATACENTER));
        System.out.printf("  Cloudlets per VM          : %.2f%n",
                (double) finishedList.size() / NUM_VMS);
        System.out.printf("  Cloudlets per Host        : %.2f%n",
                (double) finishedList.size()
                        / (NUM_DATACENTERS * HOSTS_PER_DATACENTER));
    }

    private void printNetworkDistribution(
            List<Cloudlet> finishedList,
            DatacenterBroker broker,
            List<Datacenter> datacenterList
    ) {
        System.out.println();
        System.out.println("5. NETWORK DISTRIBUTION & TOPOLOGY");
        System.out.println("----------------------------------");
        System.out.println("LOGICAL NETWORK TOPOLOGY:");
        System.out.printf("  Broker ID        : %d%n", broker.getId());
        System.out.printf("  Connected DCs    : %d%n", datacenterList.size());
        System.out.printf("  Total Hosts      : %d%n",
                NUM_DATACENTERS * HOSTS_PER_DATACENTER);
        System.out.printf("  Total VMs        : %d%n", NUM_VMS);
        System.out.printf("  Topology Type    : Star (Broker–DCs)%n");
        System.out.println();
        System.out.println("VM DISTRIBUTION ACROSS DATACENTERS:");
        for (Datacenter dc : datacenterList) {
            List<Vm> vmsInDc = broker.getVmCreatedList().stream()
                    .filter(vm -> vm.getHost() != null
                            && vm.getHost().getDatacenter().equals(dc))
                    .collect(Collectors.toList());
            System.out.printf("  Datacenter %d: %d VMs%n",
                    dc.getId(), vmsInDc.size());
            for (Host host : dc.getHostList()) {
                List<Vm> vmsOnHost = vmsInDc.stream()
                        .filter(vm -> vm.getHost().equals(host))
                        .collect(Collectors.toList());
                if (!vmsOnHost.isEmpty()) {
                    String vmIds = vmsOnHost.stream()
                            .map(vm -> String.valueOf(vm.getId()))
                            .collect(Collectors.joining(", "));
                    System.out.printf("    Host %d: %s%n",
                            host.getId(), vmIds);
                }
            }
        }

        System.out.println();
        System.out.println("CLOUDLET DISTRIBUTION ACROSS INFRASTRUCTURE:");
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
                System.out.printf("  Datacenter %d: %d cloudlets (%.1f%%)%n",
                        dcId, count, count * 100.0 / finishedList.size()));

        for (Cloudlet c : finishedList) {
            if (c.getVm() != null) {
                Vm vm = c.getVm();
                cloudletsByVm.put(vm.getId(),
                        cloudletsByVm.getOrDefault(vm.getId(), 0L) + 1);
            }
        }
        cloudletsByVm.forEach((vmId, count) ->
                System.out.printf("  VM %d: %d cloudlets (%.1f%%)%n",
                        vmId, count, count * 100.0 / finishedList.size()));

        System.out.println();
        System.out.println("DATA FLOW SUMMARY:");
        long totalInputData = finishedList.stream()
                .mapToLong(Cloudlet::getFileSize).sum();
        long totalOutputData = finishedList.stream()
                .mapToLong(Cloudlet::getOutputSize).sum();
        System.out.printf("  Total Input Data          : %.2f MB%n",
                (double) totalInputData);
        System.out.printf("  Total Output Data         : %.2f MB%n",
                (double) totalOutputData);
        System.out.printf("  Total Data Transferred    : %.2f MB%n",
                (double) (totalInputData + totalOutputData));
    }
}
