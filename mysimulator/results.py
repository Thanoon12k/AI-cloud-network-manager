#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
CloudSim Simulation Report & Visualization Generator
Reads CSV outputs from java_simulator_basic1 and generates comprehensive reports with charts
"""

import os
import sys
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path
import warnings
import io

# Fix UTF-8 encoding on Windows
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

warnings.filterwarnings('ignore')

# Set style for professional-looking charts
sns.set_style("whitegrid")
plt.rcParams['figure.figsize'] = (14, 8)
plt.rcParams['font.size'] = 10
plt.rcParams['axes.labelsize'] = 11
plt.rcParams['axes.titlesize'] = 13
plt.rcParams['xtick.labelsize'] = 9
plt.rcParams['ytick.labelsize'] = 9


class CloudSimReportGenerator:
    """Generate comprehensive reports and visualizations from CloudSim CSV outputs"""

    def __init__(self, csv_dir='.'):
        """Initialize report generator with CSV directory"""
        self.csv_dir = Path(csv_dir)
        self.dfs = {}
        self.output_dir = Path('cloudsim_reports')
        self.output_dir.mkdir(exist_ok=True)
        self._load_csv_files()

    def _load_csv_files(self):
        """Load all available CSV files into DataFrames"""
        csv_files = {
            'cloudlets': 'cloudlet_metrics.csv',
            'vms': 'vm_metrics.csv',
            'hosts': 'host_metrics.csv',
            'datacenters': 'datacenter_summary.csv',
            'performance': 'performance_metrics.csv',
            'costs': 'cost_metrics.csv'
        }

        for key, filename in csv_files.items():
            filepath = self.csv_dir / filename
            if filepath.exists():
                try:
                    self.dfs[key] = pd.read_csv(filepath)
                    print(f"✓ Loaded: {filename}")
                except Exception as e:
                    print(f"✗ Error loading {filename}: {e}")
            else:
                print(f"⚠ Missing: {filename}")


    def generate_all_reports(self):
        """Generate all reports and visualizations, then create a combined report."""
        print("\n" + "="*70)
        print("  CloudSim Simulation Report & Visualization Generator")
        print("="*70 + "\n")
        # Generate individual reports
        print("→ Generating text reports...")
        self.generate_executive_summary()
        self.generate_performance_report()
        self.generate_cost_analysis()
        self.generate_resource_utilization_report()
        # Generate visualizations
        print("\n→ Generating visualizations...")
        self.plot_cloudlet_execution_analysis()
        self.plot_vm_distribution()
        self.plot_host_utilization()
        self.plot_performance_metrics()
        self.plot_cost_breakdown()
        self.plot_execution_time_distribution()
        self.plot_waiting_time_analysis()
        self.plot_datacenter_comparison()
        self.plot_resource_capacity()
        # After all individual reports are created, generate the combined report
        self.create_combined_report()
        print("\n" + "="*70)
        print(f"✓ All reports and charts saved to: {self.output_dir}/")
        print("="*70 + "\n")

    def create_combined_report(self):
        """Create a single comprehensive report aggregating all individual reports."""
        combined_path = self.output_dir / 'COMPREHENSIVE_REPORT.txt'
        with open(combined_path, 'w', encoding='utf-8') as f:
            # Add a header with timestamp
            from datetime import datetime
            now_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            f.write(f"# Comprehensive CloudSim Report\nGenerated on: {now_str}\n\n")
            # Append Executive Summary
            exec_path = self.output_dir / 'EXECUTIVE_SUMMARY.txt'
            if exec_path.exists():
                f.write("=== Executive Summary ===\n\n")
                f.write(exec_path.read_text(encoding='utf-8') + "\n\n")
            # Append Performance Report
            perf_path = self.output_dir / 'PERFORMANCE_REPORT.txt'
            if perf_path.exists():
                f.write("=== Performance Analysis ===\n\n")
                f.write(perf_path.read_text(encoding='utf-8') + "\n\n")
            # Append Cost Analysis
            cost_path = self.output_dir / 'COST_ANALYSIS.txt'
            if cost_path.exists():
                f.write("=== Cost Analysis ===\n\n")
                f.write(cost_path.read_text(encoding='utf-8') + "\n\n")
            # Append Resource Utilization
            res_path = self.output_dir / 'RESOURCE_UTILIZATION.txt'
            if res_path.exists():
                f.write("=== Resource Utilization ===\n\n")
                f.write(res_path.read_text(encoding='utf-8') + "\n\n")
        print(f"✓ Combined report created: {combined_path}")

# ... (rest of your code remains unchanged)

    # ======================== TEXT REPORTS ========================

    def generate_executive_summary(self):
        """Generate executive summary report"""
        report_file = self.output_dir / 'EXECUTIVE_SUMMARY.txt'

        with open(report_file, 'w', encoding='utf-8') as f:
            f.write("╔" + "═"*68 + "╗\n")
            f.write("║" + " "*15 + "CloudSim Simulation Executive Summary" + " "*16 + "║\n")
            f.write("╚" + "═"*68 + "╝\n\n")

            if 'performance' in self.dfs:
                perf_df = self.dfs['performance']
                perf_dict = dict(zip(perf_df['Metric'], perf_df['Value']))

                f.write("SIMULATION OVERVIEW\n")
                f.write("-" * 70 + "\n")
                f.write(f"Total Cloudlets Processed:        {perf_dict.get('Total_Cloudlets', 'N/A')}\n")
                f.write(f"Successful Executions:            {perf_dict.get('Successful_Cloudlets', 'N/A')}\n")
                f.write(f"Failed Executions:                {perf_dict.get('Failed_Cloudlets', 'N/A')}\n")
                f.write(f"Success Rate:                     {perf_dict.get('Success_Rate_Percent', 'N/A')}%\n")

            if 'cloudlets' in self.dfs:
                f.write(f"\nTotal VMs Deployed:               {self.dfs['vms'].shape[0] if 'vms' in self.dfs else 'N/A'}\n")
                f.write(f"Total Hosts Available:            {self.dfs['hosts'].shape[0] if 'hosts' in self.dfs else 'N/A'}\n")
                f.write(f"Total Datacenters:                {self.dfs['datacenters'].shape[0] if 'datacenters' in self.dfs else 'N/A'}\n")

            if 'performance' in self.dfs:
                perf_df = self.dfs['performance']
                perf_dict = dict(zip(perf_df['Metric'], perf_df['Value']))

                f.write("\nPERFORMANCE METRICS\n")
                f.write("-" * 70 + "\n")
                f.write(f"Average Execution Time:           {perf_dict.get('Avg_Execution_Time_Sec', 'N/A')} seconds\n")
                f.write(f"Average Waiting Time:             {perf_dict.get('Avg_Waiting_Time_Sec', 'N/A')} seconds\n")
                f.write(f"Min Execution Time:               {perf_dict.get('Min_Execution_Time_Sec', 'N/A')} seconds\n")
                f.write(f"Max Execution Time:               {perf_dict.get('Max_Execution_Time_Sec', 'N/A')} seconds\n")
                f.write(f"Total Execution Time:             {perf_dict.get('Total_Execution_Time_Sec', 'N/A')} seconds\n")

            if 'costs' in self.dfs:
                cost_df = self.dfs['costs']
                cost_dict = dict(zip(cost_df['Cost_Type'], cost_df['Value_USD']))

                f.write("\nCOST ANALYSIS\n")
                f.write("-" * 70 + "\n")
                f.write(f"Compute Cost:                     ${cost_dict.get('Compute_Cost', 'N/A')}\n")
                f.write(f"Memory Cost:                      ${cost_dict.get('Memory_Cost', 'N/A')}\n")
                f.write(f"Storage Cost:                     ${cost_dict.get('Storage_Cost', 'N/A')}\n")
                f.write(f"Bandwidth Cost:                   ${cost_dict.get('Bandwidth_Cost', 'N/A')}\n")
                f.write(f"Total Cost:                       ${cost_dict.get('Total_Cost', 'N/A')}\n")

                if 'Cost_Per_Cloudlet' in cost_dict:
                    f.write(f"Cost Per Cloudlet:                ${cost_dict.get('Cost_Per_Cloudlet', 'N/A')}\n")

            if 'datacenters' in self.dfs:
                f.write("\nDATACENTER RESOURCES\n")
                f.write("-" * 70 + "\n")
                dc_df = self.dfs['datacenters']
                for idx, row in dc_df.iterrows():
                    f.write(f"\n{row['Datacenter']}:\n")
                    f.write(f"  Hosts:        {row['Total_Hosts']}\n")
                    f.write(f"  Processing Elements (PEs):  {row['Total_PEs']}\n")
                    f.write(f"  VMs Allocated:  {row['Total_VMs']}\n")
                    f.write(f"  RAM:          {row['Total_RAM_MB']} MB\n")
                    f.write(f"  Bandwidth:    {row['Total_BW_Mbps']} Mbps\n")
                    f.write(f"  Storage:      {row['Total_Storage_MB']} MB\n")

            f.write("\n" + "="*70 + "\n")
            f.write("Report generated by CloudSim Visualization Tool\n")

        print(f"✓ Executive Summary: {report_file}")

    def generate_performance_report(self):
        """Generate detailed performance report"""
        report_file = self.output_dir / 'PERFORMANCE_REPORT.txt'

        with open(report_file, 'w', encoding='utf-8') as f:
            f.write("╔" + "═"*68 + "╗\n")
            f.write("║" + " "*20 + "Performance Analysis Report" + " "*21 + "║\n")
            f.write("╚" + "═"*68 + "╝\n\n")

            if 'cloudlets' in self.dfs:
                cl_df = self.dfs['cloudlets']
                successful = cl_df[cl_df['Status'] == 'SUCCESS'].shape[0]
                failed = cl_df[cl_df['Status'] != 'SUCCESS'].shape[0]

                f.write("CLOUDLET EXECUTION SUMMARY\n")
                f.write("-" * 70 + "\n")
                f.write(f"Total Cloudlets:                  {cl_df.shape[0]}\n")
                f.write(f"Successfully Executed:            {successful}\n")
                f.write(f"Failed:                           {failed}\n")
                f.write(f"Success Rate:                     {(successful/max(cl_df.shape[0], 1)*100):.2f}%\n\n")

                f.write("EXECUTION TIME STATISTICS (seconds)\n")
                f.write("-" * 70 + "\n")
                exec_times = cl_df['Actual_CPU_Time'].describe()
                f.write(f"Mean:                             {exec_times['mean']:.4f}\n")
                f.write(f"Median:                           {cl_df['Actual_CPU_Time'].median():.4f}\n")
                f.write(f"Std Dev:                          {exec_times['std']:.4f}\n")
                f.write(f"Min:                              {exec_times['min']:.4f}\n")
                f.write(f"Max:                              {exec_times['max']:.4f}\n")
                f.write(f"25th Percentile:                  {exec_times['25%']:.4f}\n")
                f.write(f"75th Percentile:                  {exec_times['75%']:.4f}\n\n")

                f.write("WAITING TIME STATISTICS (seconds)\n")
                f.write("-" * 70 + "\n")
                wait_times = cl_df['Waiting_Time'].describe()
                f.write(f"Mean:                             {wait_times['mean']:.4f}\n")
                f.write(f"Median:                           {cl_df['Waiting_Time'].median():.4f}\n")
                f.write(f"Std Dev:                          {wait_times['std']:.4f}\n")
                f.write(f"Min:                              {wait_times['min']:.4f}\n")
                f.write(f"Max:                              {wait_times['max']:.4f}\n\n")

                f.write("DISTRIBUTION BY DATACENTER\n")
                f.write("-" * 70 + "\n")
                dc_dist = cl_df['Datacenter_ID'].value_counts().sort_index()
                for dc_id, count in dc_dist.items():
                    f.write(f"Datacenter {dc_id}:                  {count} cloudlets ({count/len(cl_df)*100:.1f}%)\n")

                f.write("\nDISTRIBUTION BY VM\n")
                f.write("-" * 70 + "\n")
                vm_dist = cl_df['VM_ID'].value_counts().sort_index()
                f.write(f"Total VMs Used:                   {len(vm_dist)}\n")
                f.write(f"Average Cloudlets per VM:        {len(cl_df)/max(len(vm_dist), 1):.2f}\n")
                f.write(f"Min Cloudlets per VM:            {vm_dist.min()}\n")
                f.write(f"Max Cloudlets per VM:            {vm_dist.max()}\n")

        print(f"✓ Performance Report: {report_file}")

    def generate_cost_analysis(self):
        """Generate cost analysis report"""
        report_file = self.output_dir / 'COST_ANALYSIS.txt'

        with open(report_file, 'w', encoding='utf-8') as f:
            f.write("╔" + "═"*68 + "╗\n")
            f.write("║" + " "*22 + "Cost Analysis Report" + " "*26 + "║\n")
            f.write("╚" + "═"*68 + "╝\n\n")

            if 'costs' in self.dfs:
                cost_df = self.dfs['costs']
                total_cost = float(cost_df[cost_df['Cost_Type'] == 'Total_Cost']['Value_USD'].values[0]) if 'Total_Cost' in cost_df['Cost_Type'].values else 0

                f.write("COST BREAKDOWN\n")
                f.write("-" * 70 + "\n")

                for idx, row in cost_df.iterrows():
                    cost_type = row['Cost_Type']
                    cost_value = float(row['Value_USD'])
                    if cost_type == 'Total_Cost':
                        f.write(f"\n{'='*70}\n")
                    percentage = (cost_value / max(total_cost, 1) * 100) if cost_type != 'Total_Cost' else 100
                    f.write(f"{cost_type:30} ${cost_value:>12.2f}  ({percentage:>5.1f}%)\n")

            if 'cloudlets' in self.dfs:
                cl_count = len(self.dfs['cloudlets'])
                if 'costs' in self.dfs and cl_count > 0:
                    cost_df = self.dfs['costs']
                    cost_per_cl = float(cost_df[cost_df['Cost_Type'] == 'Cost_Per_Cloudlet']['Value_USD'].values[0]) if 'Cost_Per_Cloudlet' in cost_df['Cost_Type'].values else 0

                    f.write("\nCOST EFFICIENCY METRICS\n")
                    f.write("-" * 70 + "\n")
                    f.write(f"Total Cloudlets:                  {cl_count}\n")
                    f.write(f"Cost Per Cloudlet:                ${cost_per_cl:.4f}\n")
                    f.write(f"Cost Per VM Hour (estimated):     $N/A\n")

            f.write("\nCOST OPTIMIZATION SUMMARY\n")
            f.write("-" * 70 + "\n")
            f.write("+ VM resources optimized (reduced MIPS, RAM, and bandwidth)\n")
            f.write("+ Host resources reduced (fewer PEs, smaller storage)\n")
            f.write("+ Cost rates reduced by 50-90% compared to standard configs\n")
            f.write("+ Multi-datacenter deployment for better cost distribution\n")

        print(f"✓ Cost Analysis: {report_file}")

    def generate_resource_utilization_report(self):
        """Generate resource utilization report"""
        report_file = self.output_dir / 'RESOURCE_UTILIZATION.txt'

        with open(report_file, 'w', encoding='utf-8') as f:
            f.write("╔" + "═"*68 + "╗\n")
            f.write("║" + " "*18 + "Resource Utilization Report" + " "*23 + "║\n")
            f.write("╚" + "═"*68 + "╝\n\n")

            if 'vms' in self.dfs:
                vm_df = self.dfs['vms']
                f.write("VM DEPLOYMENT SUMMARY\n")
                f.write("-" * 70 + "\n")
                f.write(f"Total VMs:                        {len(vm_df)}\n")
                f.write(f"Active VMs:                       {len(vm_df[vm_df['Status'] == 'ACTIVE'])}\n")
                f.write(f"Migrating VMs:                    {len(vm_df[vm_df['Status'] == 'MIGRATING'])}\n\n")

                f.write("VM RESOURCE SPECIFICATIONS\n")
                f.write("-" * 70 + "\n")
                f.write(f"Average MIPS:                     {vm_df['MIPS'].mean():.0f}\n")
                f.write(f"Average RAM:                      {vm_df['RAM_MB'].mean():.0f} MB\n")
                f.write(f"Average Bandwidth:                {vm_df['BW_Mbps'].mean():.0f} Mbps\n")
                f.write(f"Total Compute Capacity:           {vm_df['MIPS'].sum():.0f} MIPS\n")
                f.write(f"Total Memory Capacity:            {vm_df['RAM_MB'].sum():.0f} MB\n\n")

                f.write("HOST ALLOCATION\n")
                f.write("-" * 70 + "\n")
                host_allocation = vm_df.groupby('Host_ID').size()
                for host_id, count in host_allocation.items():
                    f.write(f"Host {host_id}:                       {count} VMs\n")

            if 'hosts' in self.dfs:
                host_df = self.dfs['hosts']
                f.write("\nHOST CAPACITY SUMMARY\n")
                f.write("-" * 70 + "\n")
                f.write(f"Total Hosts:                      {len(host_df)}\n")
                f.write(f"Total PEs Available:              {host_df['PEs'].sum()}\n")
                f.write(f"Total RAM Available:              {host_df['RAM_MB'].sum()} MB\n")
                f.write(f"Total Storage Available:          {host_df['Storage_MB'].sum()} MB\n")
                f.write(f"Total Bandwidth Available:        {host_df['BW_Mbps'].sum()} Mbps\n\n")

                if 'vms' in self.dfs:
                    vm_df = self.dfs['vms']
                    total_vm_ram = vm_df['RAM_MB'].sum()
                    total_host_ram = host_df['RAM_MB'].sum()
                    ram_util = (total_vm_ram / max(total_host_ram, 1) * 100)

                    f.write("UTILIZATION RATES\n")
                    f.write("-" * 70 + "\n")
                    f.write(f"RAM Utilization:                  {ram_util:.1f}%\n")
                    f.write(f"Host Allocation Rate:             {host_df['VMs_Allocated'].sum()/max(len(host_df), 1)*100:.1f}%\n")

            if 'datacenters' in self.dfs:
                dc_df = self.dfs['datacenters']
                f.write("\nDATACENTER STATISTICS\n")
                f.write("-" * 70 + "\n")
                for idx, row in dc_df.iterrows():
                    f.write(f"\n{row['Datacenter']}:\n")
                    f.write(f"  Hosts:                 {row['Total_Hosts']}\n")
                    f.write(f"  PEs:                   {row['Total_PEs']}\n")
                    f.write(f"  VMs Allocated:         {row['Total_VMs']}\n")
                    f.write(f"  Memory:                {row['Total_RAM_MB']} MB\n")
                    f.write(f"  Storage:               {row['Total_Storage_MB']} MB\n")

        print(f"✓ Resource Utilization: {report_file}")

    # ======================== VISUALIZATION CHARTS ========================

    def plot_cloudlet_execution_analysis(self):
        """Plot cloudlet execution analysis"""
        if 'cloudlets' not in self.dfs:
            return

        fig, axes = plt.subplots(2, 2, figsize=(15, 10))
        fig.suptitle('Cloudlet Execution Analysis', fontsize=16, fontweight='bold')

        cl_df = self.dfs['cloudlets']

        # 1. Status Distribution (Pie)
        status_counts = cl_df['Status'].value_counts()
        axes[0, 0].pie(status_counts, labels=status_counts.index, autopct='%1.1f%%',
                       colors=['#2ecc71', '#e74c3c', '#f39c12'], startangle=90)
        axes[0, 0].set_title('Cloudlet Status Distribution')

        # 2. Execution Time by Datacenter (Box)
        cl_df.boxplot(column='Actual_CPU_Time', by='Datacenter_ID', ax=axes[0, 1])
        axes[0, 1].set_title('Execution Time Distribution by Datacenter')
        axes[0, 1].set_xlabel('Datacenter ID')
        axes[0, 1].set_ylabel('Execution Time (seconds)')
        plt.sca(axes[0, 1])
        plt.xticks(rotation=0)

        # 3. Waiting Time by Datacenter (Box)
        cl_df.boxplot(column='Waiting_Time', by='Datacenter_ID', ax=axes[1, 0])
        axes[1, 0].set_title('Waiting Time Distribution by Datacenter')
        axes[1, 0].set_xlabel('Datacenter ID')
        axes[1, 0].set_ylabel('Waiting Time (seconds)')
        plt.sca(axes[1, 0])
        plt.xticks(rotation=0)

        # 4. Execution vs Waiting Time (Scatter)
        axes[1, 1].scatter(cl_df['Actual_CPU_Time'], cl_df['Waiting_Time'],
                          alpha=0.6, c=cl_df['Datacenter_ID'], cmap='viridis', s=50)
        axes[1, 1].set_xlabel('Execution Time (seconds)')
        axes[1, 1].set_ylabel('Waiting Time (seconds)')
        axes[1, 1].set_title('Execution Time vs Waiting Time')
        axes[1, 1].grid(True, alpha=0.3)

        plt.tight_layout()
        plt.savefig(self.output_dir / 'cloudlet_execution_analysis.png', dpi=300, bbox_inches='tight')
        print("✓ Chart: cloudlet_execution_analysis.png")
        plt.close()

    def plot_vm_distribution(self):
        """Plot VM distribution across hosts and datacenters"""
        if 'vms' not in self.dfs:
            return

        fig, axes = plt.subplots(2, 2, figsize=(15, 10))
        fig.suptitle('VM Distribution & Allocation', fontsize=16, fontweight='bold')

        vm_df = self.dfs['vms']

        # 1. VMs per Host (Bar)
        vm_per_host = vm_df['Host_ID'].value_counts().sort_index()
        axes[0, 0].bar(vm_per_host.index.astype(str), vm_per_host.values, color='#3498db', alpha=0.8)
        axes[0, 0].set_xlabel('Host ID')
        axes[0, 0].set_ylabel('Number of VMs')
        axes[0, 0].set_title('VMs Allocated per Host')
        axes[0, 0].grid(axis='y', alpha=0.3)

        # 2. VM Status Distribution (Pie)
        status_dist = vm_df['Status'].value_counts()
        axes[0, 1].pie(status_dist, labels=status_dist.index, autopct='%1.1f%%',
                       colors=['#2ecc71', '#f39c12'], startangle=90)
        axes[0, 1].set_title('VM Status Distribution')

        # 3. MIPS Allocation (Bar)
        mips_by_datacenter = vm_df.groupby('Datacenter')['MIPS'].sum()
        axes[1, 0].bar(range(len(mips_by_datacenter)), mips_by_datacenter.values,
                      color='#e74c3c', alpha=0.8)
        axes[1, 0].set_xticks(range(len(mips_by_datacenter)))
        axes[1, 0].set_xticklabels(mips_by_datacenter.index, rotation=45, ha='right')
        axes[1, 0].set_ylabel('Total MIPS')
        axes[1, 0].set_title('Compute Capacity by Datacenter')
        axes[1, 0].grid(axis='y', alpha=0.3)

        # 4. RAM Allocation (Bar)
        ram_by_datacenter = vm_df.groupby('Datacenter')['RAM_MB'].sum()
        axes[1, 1].bar(range(len(ram_by_datacenter)), ram_by_datacenter.values,
                      color='#9b59b6', alpha=0.8)
        axes[1, 1].set_xticks(range(len(ram_by_datacenter)))
        axes[1, 1].set_xticklabels(ram_by_datacenter.index, rotation=45, ha='right')
        axes[1, 1].set_ylabel('Total RAM (MB)')
        axes[1, 1].set_title('Memory Allocation by Datacenter')
        axes[1, 1].grid(axis='y', alpha=0.3)

        plt.tight_layout()
        plt.savefig(self.output_dir / 'vm_distribution.png', dpi=300, bbox_inches='tight')
        print("✓ Chart: vm_distribution.png")
        plt.close()

    def plot_host_utilization(self):
        """Plot host resource utilization"""
        if 'hosts' not in self.dfs:
            return

        fig, axes = plt.subplots(2, 2, figsize=(15, 10))
        fig.suptitle('Host Resource Utilization', fontsize=16, fontweight='bold')

        host_df = self.dfs['hosts']

        # 1. VMs per Host (Bar)
        axes[0, 0].bar(host_df['Host_ID'].astype(str), host_df['VMs_Allocated'], color='#1abc9c', alpha=0.8)
        axes[0, 0].set_xlabel('Host ID')
        axes[0, 0].set_ylabel('Number of VMs')
        axes[0, 0].set_title('VM Allocation per Host')
        axes[0, 0].grid(axis='y', alpha=0.3)

        # 2. PEs per Host (Bar)
        axes[0, 1].bar(host_df['Host_ID'].astype(str), host_df['PEs'], color='#f1c40f', alpha=0.8)
        axes[0, 1].set_xlabel('Host ID')
        axes[0, 1].set_ylabel('Number of PEs')
        axes[0, 1].set_title('Processing Elements per Host')
        axes[0, 1].grid(axis='y', alpha=0.3)

        # 3. RAM Capacity (Bar)
        axes[1, 0].bar(host_df['Host_ID'].astype(str), host_df['RAM_MB'], color='#e67e22', alpha=0.8)
        axes[1, 0].set_xlabel('Host ID')
        axes[1, 0].set_ylabel('RAM (MB)')
        axes[1, 0].set_title('RAM Capacity per Host')
        axes[1, 0].grid(axis='y', alpha=0.3)

        # 4. Storage Capacity (Bar)
        axes[1, 1].bar(host_df['Host_ID'].astype(str), host_df['Storage_MB']/1024, color='#34495e', alpha=0.8)
        axes[1, 1].set_xlabel('Host ID')
        axes[1, 1].set_ylabel('Storage (GB)')
        axes[1, 1].set_title('Storage Capacity per Host')
        axes[1, 1].grid(axis='y', alpha=0.3)

        plt.tight_layout()
        plt.savefig(self.output_dir / 'host_utilization.png', dpi=300, bbox_inches='tight')
        print("✓ Chart: host_utilization.png")
        plt.close()

    def plot_performance_metrics(self):
        """Plot overall performance metrics"""
        if 'performance' not in self.dfs:
            return

        perf_df = self.dfs['performance']
        perf_dict = dict(zip(perf_df['Metric'], perf_df['Value']))

        fig, axes = plt.subplots(2, 2, figsize=(15, 10))
        fig.suptitle('Simulation Performance Metrics', fontsize=16, fontweight='bold')

        # 1. Execution Time Metrics (Bar)
        exec_metrics = {
            'Avg': float(perf_dict.get('Avg_Execution_Time_Sec', 0)),
            'Min': float(perf_dict.get('Min_Execution_Time_Sec', 0)),
            'Max': float(perf_dict.get('Max_Execution_Time_Sec', 0))
        }
        axes[0, 0].bar(exec_metrics.keys(), exec_metrics.values(), color=['#3498db', '#2ecc71', '#e74c3c'], alpha=0.8)
        axes[0, 0].set_ylabel('Time (seconds)')
        axes[0, 0].set_title('Execution Time Metrics')
        axes[0, 0].grid(axis='y', alpha=0.3)

        # 2. Cloudlet Success Rate (Pie)
        total = float(perf_dict.get('Total_Cloudlets', 1))
        successful = float(perf_dict.get('Successful_Cloudlets', 0))
        failed = total - successful
        axes[0, 1].pie([successful, failed], labels=['Successful', 'Failed'],
                       autopct='%1.1f%%', colors=['#2ecc71', '#e74c3c'], startangle=90)
        axes[0, 1].set_title('Cloudlet Execution Status')

        # 3. Waiting vs Execution (Bar)
        time_comparison = {
            'Avg Waiting': float(perf_dict.get('Avg_Waiting_Time_Sec', 0)),
            'Avg Execution': float(perf_dict.get('Avg_Execution_Time_Sec', 0))
        }
        axes[1, 0].bar(time_comparison.keys(), time_comparison.values(), color=['#f39c12', '#9b59b6'], alpha=0.8)
        axes[1, 0].set_ylabel('Time (seconds)')
        axes[1, 0].set_title('Average Waiting vs Execution Time')
        axes[1, 0].grid(axis='y', alpha=0.3)

        # 4. Resource Utilization (Text)
        axes[1, 1].axis('off')
        stats_text = f"""
PERFORMANCE SUMMARY

Total Cloudlets:        {int(total)}
Successful:             {int(successful)}
Failed:                 {int(failed)}
Success Rate:           {perf_dict.get('Success_Rate_Percent', 'N/A')}%

Total VMs Used:         {perf_dict.get('Total_VMs_Used', 'N/A')}
Total Exec Time:        {perf_dict.get('Total_Execution_Time_Sec', 'N/A')}s
        """
        axes[1, 1].text(0.1, 0.5, stats_text, fontsize=11, family='monospace',
                       verticalalignment='center', bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5))

        plt.tight_layout()
        plt.savefig(self.output_dir / 'performance_metrics.png', dpi=300, bbox_inches='tight')
        print("✓ Chart: performance_metrics.png")
        plt.close()

    def plot_cost_breakdown(self):
        """Plot cost breakdown analysis"""
        if 'costs' not in self.dfs:
            return

        fig, axes = plt.subplots(1, 2, figsize=(14, 6))
        fig.suptitle('Cost Analysis & Breakdown', fontsize=16, fontweight='bold')

        cost_df = self.dfs['costs']
        cost_df = cost_df[cost_df['Cost_Type'] != 'Total_Cost']

        # 1. Cost Breakdown (Pie)
        axes[0].pie(cost_df['Value_USD'], labels=cost_df['Cost_Type'],
                   autopct='%1.1f%%', startangle=90)
        axes[0].set_title('Cost Distribution')

        # 2. Cost Breakdown (Bar)
        axes[1].barh(cost_df['Cost_Type'], cost_df['Value_USD'], color=['#3498db', '#e74c3c', '#f39c12', '#9b59b6'], alpha=0.8)
        axes[1].set_xlabel('Cost (USD)')
        axes[1].set_title('Cost by Category')
        axes[1].grid(axis='x', alpha=0.3)

        plt.tight_layout()
        plt.savefig(self.output_dir / 'cost_breakdown.png', dpi=300, bbox_inches='tight')
        print("✓ Chart: cost_breakdown.png")
        plt.close()

    def plot_execution_time_distribution(self):
        """Plot execution time distribution"""
        if 'cloudlets' not in self.dfs:
            return

        fig, axes = plt.subplots(1, 2, figsize=(14, 6))
        fig.suptitle('Execution Time Distribution Analysis', fontsize=16, fontweight='bold')

        cl_df = self.dfs['cloudlets']

        # 1. Histogram
        axes[0].hist(cl_df['Actual_CPU_Time'], bins=30, color='#3498db', alpha=0.7, edgecolor='black')
        axes[0].set_xlabel('Execution Time (seconds)')
        axes[0].set_ylabel('Frequency')
        axes[0].set_title('Execution Time Histogram')
        axes[0].grid(axis='y', alpha=0.3)

        # 2. Distribution by Datacenter (Violin)
        datacenter_groups = [cl_df[cl_df['Datacenter_ID'] == dc]['Actual_CPU_Time'].values
                            for dc in sorted(cl_df['Datacenter_ID'].unique())]
        parts = axes[1].violinplot(datacenter_groups, positions=range(len(datacenter_groups)), showmeans=True)
        axes[1].set_xlabel('Datacenter ID')
        axes[1].set_ylabel('Execution Time (seconds)')
        axes[1].set_title('Execution Time Distribution by Datacenter')
        axes[1].set_xticks(range(len(datacenter_groups)))
        axes[1].grid(axis='y', alpha=0.3)

        plt.tight_layout()
        plt.savefig(self.output_dir / 'execution_time_distribution.png', dpi=300, bbox_inches='tight')
        print("✓ Chart: execution_time_distribution.png")
        plt.close()

    def plot_waiting_time_analysis(self):
        """Plot waiting time analysis"""
        if 'cloudlets' not in self.dfs:
            return

        fig, axes = plt.subplots(2, 2, figsize=(15, 10))
        fig.suptitle('Cloudlet Waiting Time Analysis', fontsize=16, fontweight='bold')

        cl_df = self.dfs['cloudlets']

        # 1. Waiting Time Histogram
        axes[0, 0].hist(cl_df['Waiting_Time'], bins=30, color='#e74c3c', alpha=0.7, edgecolor='black')
        axes[0, 0].set_xlabel('Waiting Time (seconds)')
        axes[0, 0].set_ylabel('Frequency')
        axes[0, 0].set_title('Waiting Time Distribution')
        axes[0, 0].grid(axis='y', alpha=0.3)

        # 2. Waiting Time by Datacenter (Box)
        cl_df.boxplot(column='Waiting_Time', by='Datacenter_ID', ax=axes[0, 1])
        axes[0, 1].set_title('Waiting Time by Datacenter')
        axes[0, 1].set_xlabel('Datacenter ID')
        axes[0, 1].set_ylabel('Waiting Time (seconds)')
        plt.sca(axes[0, 1])
        plt.xticks(rotation=0)

        # 3. Waiting Time by VM (Box)
        vm_groups = [cl_df[cl_df['VM_ID'] == vm]['Waiting_Time'].values
                     for vm in sorted(cl_df['VM_ID'].unique())[:10]]
        axes[1, 0].boxplot(vm_groups)
        axes[1, 0].set_xlabel('VM ID (Top 10)')
        axes[1, 0].set_ylabel('Waiting Time (seconds)')
        axes[1, 0].set_title('Waiting Time by VM (Top 10)')
        axes[1, 0].grid(axis='y', alpha=0.3)

        # 4. Cumulative Waiting Time
        import numpy as np
        sorted_waiting = np.sort(cl_df['Waiting_Time'].values)
        cumulative = np.cumsum(sorted_waiting)
        axes[1, 1].plot(range(len(cumulative)), cumulative, color='#9b59b6', linewidth=2)
        axes[1, 1].fill_between(range(len(cumulative)), cumulative, alpha=0.3, color='#9b59b6')
        axes[1, 1].set_xlabel('Cloudlet Index (sorted)')
        axes[1, 1].set_ylabel('Cumulative Waiting Time (seconds)')
        axes[1, 1].set_title('Cumulative Waiting Time')
        axes[1, 1].grid(alpha=0.3)

        plt.tight_layout()
        plt.savefig(self.output_dir / 'waiting_time_analysis.png', dpi=300, bbox_inches='tight')
        print("✓ Chart: waiting_time_analysis.png")
        plt.close()

    def plot_datacenter_comparison(self):
        """Plot datacenter comparison"""
        if 'datacenters' not in self.dfs:
            return

        fig, axes = plt.subplots(2, 2, figsize=(15, 10))
        fig.suptitle('Datacenter Resource Comparison', fontsize=16, fontweight='bold')

        dc_df = self.dfs['datacenters']

        # 1. Total Hosts (Bar)
        axes[0, 0].bar(dc_df['Datacenter'], dc_df['Total_Hosts'], color='#3498db', alpha=0.8)
        axes[0, 0].set_ylabel('Number of Hosts')
        axes[0, 0].set_title('Hosts per Datacenter')
        axes[0, 0].grid(axis='y', alpha=0.3)
        plt.setp(axes[0, 0].xaxis.get_majorticklabels(), rotation=45, ha='right')

        # 2. Total PEs (Bar)
        axes[0, 1].bar(dc_df['Datacenter'], dc_df['Total_PEs'], color='#2ecc71', alpha=0.8)
        axes[0, 1].set_ylabel('Total PEs')
        axes[0, 1].set_title('Processing Elements per Datacenter')
        axes[0, 1].grid(axis='y', alpha=0.3)
        plt.setp(axes[0, 1].xaxis.get_majorticklabels(), rotation=45, ha='right')

        # 3. Total RAM (Bar)
        axes[1, 0].bar(dc_df['Datacenter'], dc_df['Total_RAM_MB']/1024, color='#e74c3c', alpha=0.8)
        axes[1, 0].set_ylabel('Total RAM (GB)')
        axes[1, 0].set_title('Memory per Datacenter')
        axes[1, 0].grid(axis='y', alpha=0.3)
        plt.setp(axes[1, 0].xaxis.get_majorticklabels(), rotation=45, ha='right')

        # 4. Total Storage (Bar)
        axes[1, 1].bar(dc_df['Datacenter'], dc_df['Total_Storage_MB']/1024, color='#f39c12', alpha=0.8)
        axes[1, 1].set_ylabel('Total Storage (GB)')
        axes[1, 1].set_title('Storage per Datacenter')
        axes[1, 1].grid(axis='y', alpha=0.3)
        plt.setp(axes[1, 1].xaxis.get_majorticklabels(), rotation=45, ha='right')

        plt.tight_layout()
        plt.savefig(self.output_dir / 'datacenter_comparison.png', dpi=300, bbox_inches='tight')
        print("✓ Chart: datacenter_comparison.png")
        plt.close()

    def plot_resource_capacity(self):
        """Plot overall resource capacity"""
        import numpy as np
        
        fig, axes = plt.subplots(2, 2, figsize=(15, 10))
        fig.suptitle('Overall Resource Capacity & Allocation', fontsize=16, fontweight='bold')

        # Data aggregation
        total_hosts = self.dfs['hosts'].shape[0] if 'hosts' in self.dfs else 0
        total_vms = self.dfs['vms'].shape[0] if 'vms' in self.dfs else 0
        total_cloudlets = self.dfs['cloudlets'].shape[0] if 'cloudlets' in self.dfs else 0

        total_host_mips = self.dfs['hosts']['Total_MIPS_Capacity'].sum() if 'hosts' in self.dfs else 0
        total_vm_mips = self.dfs['vms']['MIPS'].sum() if 'vms' in self.dfs else 0

        # 1. Resource Count (Bar)
        resources = ['Hosts', 'VMs', 'Cloudlets']
        counts = [total_hosts, total_vms, total_cloudlets]
        axes[0, 0].bar(resources, counts, color=['#3498db', '#2ecc71', '#e74c3c'], alpha=0.8)
        axes[0, 0].set_ylabel('Count')
        axes[0, 0].set_title('Resource Counts')
        axes[0, 0].grid(axis='y', alpha=0.3)
        for i, v in enumerate(counts):
            axes[0, 0].text(i, v + max(counts)*0.02, str(int(v)), ha='center', fontweight='bold')

        # 2. MIPS Capacity (Bar)
        axes[0, 1].bar(['Host Capacity', 'VM Allocation'], [total_host_mips, total_vm_mips],
                      color=['#f39c12', '#9b59b6'], alpha=0.8)
        axes[0, 1].set_ylabel('Total MIPS')
        axes[0, 1].set_title('Compute Capacity vs Allocation')
        axes[0, 1].grid(axis='y', alpha=0.3)

        # 3. RAM Allocation (Pie)
        if 'hosts' in self.dfs and 'vms' in self.dfs:
            total_host_ram = self.dfs['hosts']['RAM_MB'].sum()
            total_vm_ram = self.dfs['vms']['RAM_MB'].sum()
            axes[1, 0].pie([total_vm_ram, total_host_ram - total_vm_ram],
                          labels=['Allocated', 'Available'],
                          autopct='%1.1f%%', colors=['#e74c3c', '#2ecc71'], startangle=90)
            axes[1, 0].set_title(f'Memory Utilization ({total_vm_ram}/{total_host_ram} MB)')

        # 4. Summary Text
        axes[1, 1].axis('off')
        summary = f"""
INFRASTRUCTURE SUMMARY

Physical Resources:
  Total Hosts:          {total_hosts}
  Total PEs:            {self.dfs['hosts']['PEs'].sum() if 'hosts' in self.dfs else 'N/A'}
  Total Host MIPS:      {int(total_host_mips)}

Virtual Resources:
  Total VMs:            {total_vms}
  Total Cloudlets:      {total_cloudlets}
  VM MIPS Allocation:   {int(total_vm_mips)}

Utilization:
  MIPS Utilization:     {total_vm_mips/max(total_host_mips, 1)*100:.1f}%
  VMs/Host Ratio:       {total_vms/max(total_hosts, 1):.1f}
        """
        axes[1, 1].text(0.05, 0.5, summary, fontsize=10, family='monospace',
                       verticalalignment='center', bbox=dict(boxstyle='round', facecolor='lightblue', alpha=0.5))

        plt.tight_layout()
        plt.savefig(self.output_dir / 'resource_capacity.png', dpi=300, bbox_inches='tight')
        print("✓ Chart: resource_capacity.png")
        plt.close()


def main():
    """Main execution function"""
    csv_directory = '.'

    generator = CloudSimReportGenerator(csv_dir=csv_directory)
    generator.generate_all_reports()

    print("\n" + "="*70)
    print("REPORT GENERATION COMPLETE")
    print("="*70)
    print(f"\nGenerated files in: {generator.output_dir}/")
    print("\nText Reports:")
    print("  * EXECUTIVE_SUMMARY.txt")
    print("  * PERFORMANCE_REPORT.txt")
    print("  * COST_ANALYSIS.txt")
    print("  * RESOURCE_UTILIZATION.txt")
    print("\nVisualizations (PNG Charts):")
    print("  * cloudlet_execution_analysis.png")
    print("  * vm_distribution.png")
    print("  * host_utilization.png")
    print("  * performance_metrics.png")
    print("  * cost_breakdown.png")
    print("  * execution_time_distribution.png")
    print("  * waiting_time_analysis.png")
    print("  * datacenter_comparison.png")
    print("  * resource_capacity.png")
    print("\n" + "="*70 + "\n")


if __name__ == '__main__':
    main()
