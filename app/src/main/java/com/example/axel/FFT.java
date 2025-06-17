package com.example.axel;
public class FFT {
    public static void computeFFT(double[] inputReal, double[] inputImag, int n) {
        if (n == 1) return;

        double[] evenReal = new double[n / 2];
        double[] evenImag = new double[n / 2];
        double[] oddReal = new double[n / 2];
        double[] oddImag = new double[n / 2];

        for (int i = 0; i < n / 2; i++) {
            evenReal[i] = inputReal[2 * i];
            evenImag[i] = inputImag[2 * i];
            oddReal[i] = inputReal[2 * i + 1];
            oddImag[i] = inputImag[2 * i + 1];
        }

        computeFFT(evenReal, evenImag, n / 2);
        computeFFT(oddReal, oddImag, n / 2);

        for (int k = 0; k < n / 2; k++) {
            double angle = -2 * Math.PI * k / n;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            double realPart = oddReal[k] * cos - oddImag[k] * sin;
            double imagPart = oddReal[k] * sin + oddImag[k] * cos;

            inputReal[k] = evenReal[k] + realPart;
            inputImag[k] = evenImag[k] + imagPart;
            inputReal[k + n / 2] = evenReal[k] - realPart;
            inputImag[k + n / 2] = evenImag[k] - imagPart;
        }
    }
}
