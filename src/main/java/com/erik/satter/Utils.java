package com.erik.satter;

public class Utils {
    public static class Lit {
        public static int of(int row, int col, int val) {
            return (row << 8) | (col << 4) | val;
        }

        public static int compOf(int row, int col, int val) {
            return of(row, col, val) * -1;
        }

        public static int val(int lit) {
            return Math.abs(lit) & 0xf;
        }

        public static int col(int lit) {
            return (Math.abs(lit) & 0xf0) >> 4;
        }

        public static int row(int lit) {
            return (Math.abs(lit) & 0xf00) >> 8;
        }

        public static boolean isTrue(int lit) {
            return lit > 0;
        }
    }
}
