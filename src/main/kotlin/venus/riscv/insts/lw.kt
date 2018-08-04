package venus.riscv.insts

import venus.riscv.insts.dsl.LoadTypeInstruction
import venus.simulator.Simulator

val lw = LoadTypeInstruction(
        name = "lw",
        opcode = 0b0000011,
        funct3 = 0b010,
//        load16 = Simulator::loadWordwCache
        load32 = Simulator::loadWordwCache
//        load64 = Simulator::loadWordwCache
//        load128 = Simulator::loadWordwCache
)
