package com.hjz.util;

/**
 * description 基于雪花算法改造的序列，主要解决时钟回拨问题，与雪花算法中部分值的长度不同
 * id规则：共64位二进制
 * 第1位为0表示正数
 * 第2-34位用于存储10位的时间戳，单位位秒，可以使用220年左右
 * 第35-36位用于标识是否发生时钟回拨，回拨时该位为1，否则为0
 * 第37-47位共11位用于存储机器id，即一个库最多有2048个机器id可用
 * 第48-64位用于每秒内的自增id，即每秒最多可生成262144个唯一id
 *
 * @author HongJianzhou
 * @date 2024/11/29
 */
public class SequenceUtil {

    /**
     * 时钟回拨标志位
     */
    private static int clockBack = 0;

    /**
     * 时钟回拨标志位占1位
     */
    private final static int CLOCK_BACK_BITS = 1;

    /**
     * 机器码占11位，能供2的11次方=2048个服务使用
     */
    private final static int MACHINE_ID_BITS = 11;

    /**
     * 自增序列占18位
     */
    private final static int SEQUENCE_BITS = 18;

    /**
     * 最后使用的时间戳
     */
    private static long lastTimestamp = -1L;

    /**
     * 序列掩码，~是取反操作
     */
    private final static long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /**
     * 最大机器码
     */
    private final static long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS);

    /**
     * 时间戳左移位数
     */
    private final static long TIMESTAMP_LEFT_SHIFT = CLOCK_BACK_BITS + MACHINE_ID_BITS + SEQUENCE_BITS;

    /**
     * 时钟回拨控制位左移位数
     */
    private final static long CLOCK_BACK_LEFT_SHIFT = MACHINE_ID_BITS + SEQUENCE_BITS;

    /**
     * 机器码左移位数
     */
    private final static long MACHINE_ID_LEFT_SHIFT = SEQUENCE_BITS;

    /**
     * 触发时钟回拨后的值，值相对固定，所以可以提前计算出减少后续计算量
     */
    private final static long CLOCK_BACK_TRUE = 1 << CLOCK_BACK_LEFT_SHIFT;

    /**
     * 机器id，十进制
     */
    private static long machineId = -1;

    /**
     * 机器id左移后的二进制位
     */
    private static long BINARY_MACHINE_ID;

    /**
     * 序列位
     */
    private static long sequence = 0L;

    public static synchronized void init(long machineId) {
        SequenceUtil.machineId = machineId;
        BINARY_MACHINE_ID = machineId << MACHINE_ID_LEFT_SHIFT;
    }

    /**
     * 获取唯一id
     *
     * @return 0 + 时间戳 + 1位始终回拨标志（默认0） + 机器id + 序列 构成的唯一id
     */
    public synchronized static long getNextId() {
        long timestamp = System.currentTimeMillis() / 1000;
        if (timestamp < lastTimestamp) {
            // 发生时钟回拨，通过继续使用未来时间避免id的自增被破坏、多次回拨至同一时间段带来的id重复问题
            clockBack = 1;
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 该秒的用完了，等待至下一秒
            if (sequence == 0) {
                // 最后使用时间也会更新到下一秒
                lastTimestamp = tilNextSecond(lastTimestamp);
            }
            return (lastTimestamp << TIMESTAMP_LEFT_SHIFT) | CLOCK_BACK_TRUE | BINARY_MACHINE_ID | sequence;
        } else if (timestamp == lastTimestamp) {
            // 同一秒内继续使用序列
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 该秒的用完了，等待到下一秒
            if (sequence == 0) {
                lastTimestamp = tilNextSecond(lastTimestamp);
            }
            // 更新使用过的最新一秒
            lastTimestamp = timestamp;
            return (timestamp << TIMESTAMP_LEFT_SHIFT) | clockBack << CLOCK_BACK_LEFT_SHIFT | BINARY_MACHINE_ID | sequence;
        } else {
            // 新的一秒重置相关信息
            clockBack = 0;
            sequence = 0;
            lastTimestamp = timestamp;
            return (timestamp << TIMESTAMP_LEFT_SHIFT) | BINARY_MACHINE_ID | sequence;
        }
    }

    private static long tilNextSecond(long lastTimestamp) {
        long diff = (lastTimestamp + 1) * 1000 - System.currentTimeMillis();
        // 等待到下一秒
        if (diff > 0) {
            try {
                Thread.sleep(diff);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return System.currentTimeMillis() / 1000;
    }

    public static void main(String[] args) {
        SequenceUtil.init(0);
        System.out.println(SequenceUtil.getNextId());
        System.out.println(SequenceUtil.getNextId());
        System.out.println(SequenceUtil.getNextId());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println(SequenceUtil.getNextId());
    }
}
