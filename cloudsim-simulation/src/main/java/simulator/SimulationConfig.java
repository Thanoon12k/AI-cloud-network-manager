package simulator;

/**
 * OPTIMIZED CONFIGURATION
 * Goal: ~80% CPU Utilization, Reduced Static Waste, Balanced Cost Profile.
 */
public class SimulationConfig {

    // ================= INFRASTRUCTURE =================
    // Kept the same to demonstrate how much more this hardware can handle
    public static final int NUM_DATACENTERS = 3;
    public static final int HOSTS_PER_DATACENTER = 1;
    public static final int PES_PER_HOST = 5;      // 5 Cores per Host
    public static final int NUM_VMS = 15;              // 15 Total VMs
    public static final int NUM_CLOUDLETS = 2000;

  
    public static final long CLOUDLET_LENGTH = 1200; 

    public static final long CLOUDLET_FILE_SIZE = 300; 
    public static final long CLOUDLET_OUTPUT_SIZE = 300; 
    public static final int CLOUDLET_PES = 1;

    // ================= HARDWARE SPECS =================
    // Host specifications
//    public static final long HOST_MIPS = 2000;     // 2000 MIPS per Core
//    public static final long HOST_RAM = 16000;
//    public static final long HOST_STORAGE = 1_000_000;
//    public static final long HOST_BW = 100_000;

    // VM specifications
    public static final long VM_MIPS = 1000;       // 1000 MIPS per VM
    public static final int VM_PES = 1;
    public static final long VM_RAM = 2048;
    public static final long VM_BW = 10000;
//    public static final long VM_STORAGE = 10_000;

    // ================= PRICING & ENERGY =================
    public static final double COST_PER_SEC = 0.01;
    public static final double COST_PER_MEM = 0.002;
    public static final double COST_PER_STORAGE = 0.0005;
    public static final double COST_PER_BW = 0.000003;

    public static final double STATIC_POWER = 50;  // Watt
    public static final double MAX_POWER = 200;    // Watt

    private SimulationConfig() { }
}