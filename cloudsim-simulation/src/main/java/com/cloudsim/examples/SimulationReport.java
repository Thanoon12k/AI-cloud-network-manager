//package com.cloudsim.examples;
//
//import java.io.FileWriter;
//import java.io.IOException;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//import org.cloudsimplus.cloudlets.Cloudlet;
//import org.cloudsimplus.datacenters.Datacenter;
//import org.cloudsimplus.hosts.Host;
//import org.cloudsimplus.vms.Vm;
//import org.cloudsimplus.brokers.DatacenterBroker;
//
///**
// * Simulation Report Generator
// * Generates comprehensive reports in multiple formats: Console, CSV, JSON, HTML
// */
//public class SimulationReport {
//    
//    private final List<Cloudlet> finishedCloudlets;
//    private final List<Vm> vmList;
//    private final List<Datacenter> datacenterList;
//    private final DatacenterBroker broker;
//    private final long simulationTime;
//    
//    public SimulationReport(List<Cloudlet> finishedCloudlets, List<Vm> vmList, 
//                           List<Datacenter> datacenterList, DatacenterBroker broker, long simulationTime) {
//        this.finishedCloudlets = finishedCloudlets;
//        this.vmList = vmList;
//        this.datacenterList = datacenterList;
//        this.broker = broker;
//        this.simulationTime = simulationTime;
//    }
//    
//    // Generate Console Report
//    public void generateConsoleReport() {
//        System.out.println("\n" + "=".repeat(100));
//        System.out.println(" ".repeat(30) + "COMPREHENSIVE SIMULATION REPORT");
//        System.out.println("=".repeat(100));
//        printTimestamp();
//        printCloudletSummary();
//        printVmSummary();
//        printHostSummary();
//        printResourceUtilization();
//        printPowerAndCost();
//        printNetworkStatistics();
//        printPerformanceMetrics();
//        System.out.println("=".repeat(100) + "\n");
//    }
//    
//    // Generate CSV Report
//    public void generateCSVReport(String filename) {
//        try (FileWriter writer = new FileWriter(filename)) {
//            writer.write("Simulation Report - CSV Format\n");
//            writer.write("Generated: " + LocalDateTime.now() + "\n\n");
//            
//            // Cloudlet Statistics CSV
//            writer.write("CLOUDLET STATISTICS\n");
//            writer.write("Metric,Value\n");
//            writer.write("Total Cloudlets," + finishedCloudlets.size() + "\n");
//            writer.write("Success Rate," + String.format("%.2f%%", getSuccessRate()) + "\n");
//            writer.write("Average Execution Time," + String.format("%.2f seconds", getAvgExecutionTime()) + "\n");
//            writer.write("Min Execution Time," + String.format("%.2f seconds", getMinExecutionTime()) + "\n");
//            writer.write("Max Execution Time," + String.format("%.2f seconds", getMaxExecutionTime()) + "\n\n");
//            
//            // VM Statistics CSV
//            writer.write("VM STATISTICS\n");
//            writer.write("VM_ID,Datacenter,Host,RAM_MB,BW_Mbps,Status\n");
//            for (Vm vm : broker.getVmCreatedList()) {
//                writer.write(vm.getId() + "," + vm.getHost().getDatacenter().getId() + "," + 
//                           vm.getHost().getId() + "," + vm.getRam().getCapacity() + "," + 
//                           vm.getBw().getCapacity() + ",ACTIVE\n");
//            }
//            writer.write("\n");
//            
//            // Host Statistics CSV
//            writer.write("HOST STATISTICS\n");
//            writer.write("Host_ID,Datacenter,Total_MIPS,Total_RAM_MB,Total_BW_Mbps,CPU_Util_%,RAM_Util_%,BW_Util_%,Power_W,Cost_$/hr\n");
//            for (Datacenter dc : datacenterList) {
//                for (Host host : dc.getHostList()) {
//                    writer.write(host.getId() + "," + dc.getId() + "," + host.getTotalMips() + "," +
//                               host.getRam().getCapacity() + "," + host.getBw().getCapacity() + "," +
//                               String.format("%.2f", host.getCpuPercentUtilization() * 100) + "," +
//                               String.format("%.2f", getRamUtilization(host)) + "," +
//                               String.format("%.2f", getBwUtilization(host)) + "," +
//                               String.format("%.2f", calculateHostPower(host)) + "," +
//                               String.format("%.4f", calculateHostCost(host)) + "\n");
//                }
//            }
//            
//            System.out.println("✅ CSV Report saved to: " + filename);
//        } catch (IOException e) {
//            System.err.println("❌ Error writing CSV report: " + e.getMessage());
//        }
//    }
//    
//    // Generate JSON Report
//    public void generateJSONReport(String filename) {
//        try (FileWriter writer = new FileWriter(filename)) {
//            writer.write("{\n");
//            writer.write("  \"timestamp\": \"" + LocalDateTime.now() + "\",\n");
//            writer.write("  \"cloudletStatistics\": {\n");
//            writer.write("    \"total\": " + finishedCloudlets.size() + ",\n");
//            writer.write("    \"successRate\": " + String.format("%.2f", getSuccessRate()) + ",\n");
//            writer.write("    \"averageExecutionTime\": " + String.format("%.2f", getAvgExecutionTime()) + ",\n");
//            writer.write("    \"minExecutionTime\": " + String.format("%.2f", getMinExecutionTime()) + ",\n");
//            writer.write("    \"maxExecutionTime\": " + String.format("%.2f", getMaxExecutionTime()) + "\n");
//            writer.write("  },\n");
//            
//            writer.write("  \"vmStatistics\": {\n");
//            writer.write("    \"totalVMs\": " + broker.getVmCreatedList().size() + ",\n");
//            writer.write("    \"totalRAM_MB\": " + getTotalVmRam() + ",\n");
//            writer.write("    \"totalBW_Mbps\": " + getTotalVmBw() + "\n");
//            writer.write("  },\n");
//            
//            writer.write("  \"hostStatistics\": {\n");
//            writer.write("    \"totalPower_W\": " + String.format("%.2f", getTotalHostPower()) + ",\n");
//            writer.write("    \"totalCost_$/hr\": " + String.format("%.4f", getTotalHostCost()) + ",\n");
//            writer.write("    \"costPerCloudlet_$\": " + String.format("%.6f", getTotalHostCost() / Math.max(finishedCloudlets.size(), 1)) + "\n");
//            writer.write("  },\n");
//            
//            writer.write("  \"networkStatistics\": {\n");
//            writer.write("    \"totalBW_Mbps\": " + getTotalVmBw() + ",\n");
//            writer.write("    \"networkUtilization_%\": " + String.format("%.2f", getNetworkUtilization()) + "\n");
//            writer.write("  }\n");
//            writer.write("}\n");
//            
//            System.out.println("✅ JSON Report saved to: " + filename);
//        } catch (IOException e) {
//            System.err.println("❌ Error writing JSON report: " + e.getMessage());
//        }
//    }
//    
//    // Generate HTML Report
//    public void generateHTMLReport(String filename) {
//        try (FileWriter writer = new FileWriter(filename)) {
//            writer.write("<!DOCTYPE html>\n<html>\n<head>\n");
//            writer.write("<meta charset=\"UTF-8\">\n");
//            writer.write("<title>CloudSim Plus Simulation Report</title>\n");
//            writer.write("<style>\n");
//            writer.write("body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n");
//            writer.write("h1 { color: #333; text-align: center; }\n");
//            writer.write("h2 { color: #0066cc; border-bottom: 2px solid #0066cc; padding-bottom: 5px; }\n");
//            writer.write("table { border-collapse: collapse; width: 100%; margin: 20px 0; background-color: white; }\n");
//            writer.write("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }\n");
//            writer.write("th { background-color: #0066cc; color: white; }\n");
//            writer.write("tr:nth-child(even) { background-color: #f9f9f9; }\n");
//            writer.write(".metric { font-weight: bold; color: #0066cc; }\n");
//            writer.write(".value { color: #333; }\n");
//            writer.write(".success { color: green; }\n");
//            writer.write(".container { max-width: 1200px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 5px; }\n");
//            writer.write("</style>\n</head>\n<body>\n");
//            
//            writer.write("<div class=\"container\">\n");
//            writer.write("<h1>CloudSim Plus Simulation Report</h1>\n");
//            writer.write("<p style=\"text-align: center; color: #666;\">Generated: " + LocalDateTime.now() + "</p>\n");
//            
//            // Cloudlet Statistics Section
//            writer.write("<h2>Cloudlet Execution Statistics</h2>\n<table>\n");
//            writer.write("<tr><th>Metric</th><th>Value</th></tr>\n");
//            writer.write("<tr><td class=\"metric\">Total Cloudlets</td><td class=\"value\">" + finishedCloudlets.size() + "</td></tr>\n");
//            writer.write("<tr><td class=\"metric\">Success Rate</td><td class=\"value success\">" + String.format("%.2f%%", getSuccessRate()) + "</td></tr>\n");
//            writer.write("<tr><td class=\"metric\">Average Execution Time</td><td class=\"value\">" + String.format("%.2f seconds", getAvgExecutionTime()) + "</td></tr>\n");
//            writer.write("<tr><td class=\"metric\">Min Execution Time</td><td class=\"value\">" + String.format("%.2f seconds", getMinExecutionTime()) + "</td></tr>\n");
//            writer.write("<tr><td class=\"metric\">Max Execution Time</td><td class=\"value\">" + String.format("%.2f seconds", getMaxExecutionTime()) + "</td></tr>\n");
//            writer.write("</table>\n");
//            
//            // VM Statistics Section
//            writer.write("<h2>Virtual Machine Statistics</h2>\n<table>\n");
//            writer.write("<tr><th>VM ID</th><th>Datacenter</th><th>RAM (MB)</th><th>Bandwidth (Mbps)</th></tr>\n");
//            for (Vm vm : broker.getVmCreatedList()) {
//                writer.write("<tr><td>" + vm.getId() + "</td><td>" + vm.getHost().getDatacenter().getId() + 
//                           "</td><td>" + vm.getRam().getCapacity() + "</td><td>" + vm.getBw().getCapacity() + "</td></tr>\n");
//            }
//            writer.write("</table>\n");
//            
//            // Host Statistics Section
//            writer.write("<h2>Host Resource Utilization</h2>\n<table>\n");
//            writer.write("<tr><th>Host ID</th><th>Datacenter</th><th>CPU Util %</th><th>RAM Util %</th><th>BW Util %</th><th>Power (W)</th><th>Cost ($/hr)</th></tr>\n");
//            for (Datacenter dc : datacenterList) {
//                for (Host host : dc.getHostList()) {
//                    writer.write("<tr><td>" + host.getId() + "</td><td>" + dc.getId() + "</td><td>" +
//                               String.format("%.2f", host.getCpuPercentUtilization() * 100) + "</td><td>" +
//                               String.format("%.2f", getRamUtilization(host)) + "</td><td>" +
//                               String.format("%.2f", getBwUtilization(host)) + "</td><td>" +
//                               String.format("%.2f", calculateHostPower(host)) + "</td><td>" +
//                               String.format("$.4f", calculateHostCost(host)) + "</td></tr>\n");
//                }
//            }
//            writer.write("</table>\n");
//            
//            // Operations Summary
//            writer.write("<h2>Datacenter Operations Summary</h2>\n<table>\n");
//            writer.write("<tr><th>Metric</th><th>Value</th></tr>\n");
//            writer.write("<tr><td class=\"metric\">Total Hosts</td><td class=\"value\">" + getTotalHosts() + "</td></tr>\n");
//            writer.write("<tr><td class=\"metric\">Total Power Consumption</td><td class=\"value\">" + String.format("%.2f W", getTotalHostPower()) + "</td></tr>\n");
//            writer.write("<tr><td class=\"metric\">Total Cost/Hour</td><td class=\"value\">$" + String.format("%.4f", getTotalHostCost()) + "</td></tr>\n");
//            writer.write("<tr><td class=\"metric\">Cost per Cloudlet</td><td class=\"value\">$" + String.format("%.6f", getTotalHostCost() / Math.max(finishedCloudlets.size(), 1)) + "</td></tr>\n");
//            writer.write("</table>\n");
//            
//            writer.write("</div>\n</body>\n</html>\n");
//            
//            System.out.println("✅ HTML Report saved to: " + filename);
//        } catch (IOException e) {
//            System.err.println("❌ Error writing HTML report: " + e.getMessage());
//        }
//    }
//    
//    // Helper methods for calculations...
//    // [All the calculation methods from above]
//}
