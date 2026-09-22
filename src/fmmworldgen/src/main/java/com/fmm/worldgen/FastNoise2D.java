package com.fmm.worldgen;

public final class FastNoise2D {
   private static final double F2 = 0.5 * (Math.sqrt(3.0) - 1.0);
   private static final double G2 = (3.0 - Math.sqrt(3.0)) / 6.0;
   private static final int[] GRAD_X = new int[]{1, -1, 1, -1, 1, -1, 1, -1, 0, 0, 0, 0};
   private static final int[] GRAD_Z = new int[]{1, 1, -1, -1, 0, 0, 0, 0, 1, -1, 1, -1};

   private FastNoise2D() {
   }

   public static double hash(long gridX, long gridZ, long salt) {
      long seed = gridX * 87519317569972L + gridZ * 30468017453299L + salt * -7046029254386353131L;
      seed = (seed ^ seed >>> 30) * -4658895280553007687L;
      seed = (seed ^ seed >>> 27) * -7723592293110705685L;
      seed ^= seed >>> 31;
      return (seed & 281474976710655L) / 2.8147498E14F;
   }

   public static double simplex2D(double x, double z, long seed) {
      double s = (x + z) * F2;
      int i = fastFloor(x + s);
      int j = fastFloor(z + s);
      double t = (i + j) * G2;
      double x0 = x - (i - t);
      double z0 = z - (j - t);
      int i1;
      int j1;
      if (x0 > z0) {
         i1 = 1;
         j1 = 0;
      } else {
         i1 = 0;
         j1 = 1;
      }

      double x1 = x0 - i1 + G2;
      double z1 = z0 - j1 + G2;
      double x2 = x0 - 1.0 + 2.0 * G2;
      double z2 = z0 - 1.0 + 2.0 * G2;
      int gi0 = hashGrad(i, j, seed);
      int gi1 = hashGrad(i + i1, j + j1, seed);
      int gi2 = hashGrad(i + 1, j + 1, seed);
      double n0 = 0.0;
      double t0 = 0.5 - x0 * x0 - z0 * z0;
      if (t0 > 0.0) {
         t0 *= t0;
         n0 = t0 * t0 * (GRAD_X[gi0] * x0 + GRAD_Z[gi0] * z0);
      }

      double n1 = 0.0;
      double t1 = 0.5 - x1 * x1 - z1 * z1;
      if (t1 > 0.0) {
         t1 *= t1;
         n1 = t1 * t1 * (GRAD_X[gi1] * x1 + GRAD_Z[gi1] * z1);
      }

      double n2 = 0.0;
      double t2 = 0.5 - x2 * x2 - z2 * z2;
      if (t2 > 0.0) {
         t2 *= t2;
         n2 = t2 * t2 * (GRAD_X[gi2] * x2 + GRAD_Z[gi2] * z2);
      }

      return 70.0 * (n0 + n1 + n2);
   }

   public static double domainWarp(double x, double z, double frequency, double amplitude, long seed) {
      return simplex2D(x * frequency, z * frequency, seed) * amplitude;
   }

   private static int fastFloor(double v) {
      int i = (int)v;
      return v < i ? i - 1 : i;
   }

   private static int hashGrad(int x, int z, long seed) {
      long h = x * 374761393L + z * 668265263L + seed * 1274126177L;
      h = (h ^ h >>> 13) * 1274126177L;
      return (int)(Math.abs(h) % 12L);
   }
}
