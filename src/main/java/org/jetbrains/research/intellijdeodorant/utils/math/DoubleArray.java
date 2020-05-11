package org.jetbrains.research.intellijdeodorant.utils.math;


/**
 * A class for dealing with double arrays and matrices.
 * Copyright : BSD License
 *
 * @author Yann RICHET
 */

public class DoubleArray {

    /**
     * Insert any number of arrays between 2 columns of a matrix. Size of the arrays must
     * equal number of rows in the matrix.
     * Example:<br>
     * <code>
     * double[][] a = {{0,1,2,3,4},{1,7,8,9,10},{2,13,14,15,16},{3,19,20,21,22},{4,23,24,25,26}};<br>
     * double[] b = {00,11,22,33,44}, c = {55,66,77,88,99};<br>
     * double[][] z = insertColumns(a, 2, b, c);<br>
     * input matrix is:<br>
     * 0   1   2   3   4<br>
     * 1   7   8   9  10<br>
     * 2  13  14  15  16<br>
     * 3  19  20  21  22<br>
     * 4  23  24  25  26<br>
     * result is:<br>
     * 0   1   0  55   2   3   4<br>
     * 1   7  11  66   8   9  10<br>
     * 2  13  22  77  14  15  16<br>
     * 3  19  33  88  20  21  22<br>
     * 4  23  44  99  24  25  26<br>
     * </code>
     *
     * @param x Input m x n matrix.
     * @param J Index of column before which the new columns will be inserted.
     * @param y The arrays to be inserted
     * @return New matrix with added columns.
     */
    public static double[][] insertColumns(double[][] x, int J, double[]... y) {
        return transpose(insertRows(transpose(x), J, y));
    }

    /**
     * Insert any number of arrays between 2 rows of a matrix. Size of the arrays must
     * equal number of columns in the matrix.
     * Example:<br>
     * <code>
     * double[][] a = {{0,1,2,3,4},{1,7,8,9,10},{2,13,14,15,16},{3,19,20,21,22}};<br>
     * double[] b = {0,11,22,33,44}, c = {55,66,77,88,99};<br>
     * double[][] z = insertRows(a, 1, b, c);<br>
     * result is:<br>
     * 0   1   2   3   4<br>
     * 0  11  22  33  44<br>
     * 55  66  77  88  99<br>
     * 1   7   8   9  10<br>
     * 2  13  14  15  16<br>
     * 3  19  20  21  22<br>
     * </code>
     *
     * @param x Input m x n matrix.
     * @param I Index of row before which the new rows will be inserted.
     * @param y The arrays to be inserted
     * @return New matrix with added rows.
     */
    public static double[][] insertRows(double[][] x, int I, double[]... y) {
        double[][] array = new double[x.length + y.length][x[0].length];
        for (int i = 0; i < I; i++)
            System.arraycopy(x[i], 0, array[i], 0, x[i].length);
        for (int i = 0; i < y.length; i++)
            System.arraycopy(y[i], 0, array[i + I], 0, y[i].length);
        for (int i = 0; i < x.length - I; i++)
            System.arraycopy(x[i + I], 0, array[i + I + y.length], 0, x[i].length);
        return array;
    }

    /**
     * Deletes a list of columns from a matrix.
     * Example:<br>
     * <code>
     * double[][] a = {{0,1,2,3,4},{1,7,8,9,10},{2,13,14,15,16},{3,19,20,21,22},{4,23,24,25,26}};<br>
     * double[][] z = deleteColumns(a, 1, 3);<br>
     * result is:<br>
     * 0  2   4<br>
     * 1  8  10<br>
     * 2  14 16<br>
     * 3  20 22<br>
     * 4  24 26<br>
     * </code>
     *
     * @param x The input matrix
     * @param J The indices of the columns to be deleted. There must be no more indices listed
     *          than there are columns in the input matrix.
     * @return The reduced matrix.
     */
    public static double[][] deleteColumns(double[][] x, int... J) {
        // TODO improve efficiency here
        return transpose(deleteRows(transpose(x), J));
    }

    /**
     * Deletes a list of rows from a matrix.
     * Example:<br>
     * <code>
     * double[][] a = {{0,1,2,3,4},{1,7,8,9,10},{2,13,14,15,16},{3,19,20,21,22},{4,23,24,25,26}};<br>
     * double[][] z = deleteRows(a, 1, 3);<br>
     * result is:<br>
     * 0   1   2   3   4<br>
     * 2  13  14  15  16<br>
     * 4  23  24  25  26<br>
     * </code>
     *
     * @param x The input matrix
     * @param I The indices of the rows to delete.
     * @return The reduced matrix.
     */
    public static double[][] deleteRows(double[][] x, int... I) {
        double[][] array = new double[x.length - I.length][x[0].length];
        int i2 = 0;
        for (int i = 0; i < x.length; i++) {
            if (!into(i, I)) {
                System.arraycopy(x[i], 0, array[i2], 0, x[i].length);
                i2++;
            }
        }
        return array;
    }

    /**
     * Determines if a value is within an array
     *
     * @param i Value to be searched for.
     * @param I array to be searched
     * @return true if found, false if not.
     */
    private static boolean into(int i, int[] I) {
        boolean in = false;
        for (int value : I) {
            in = in || (i == value);
        }
        return in;
    }


    /**
     * Transposes an mxn matrix into an nxm matrix. Each row of the input matrix becomes a column in the
     * output matrix.
     *
     * @param M Input matrix.
     * @return Transposed version of M.
     */
    public static double[][] transpose(double[][] M) {
        double[][] tM = new double[M[0].length][M.length];
        for (int i = 0; i < tM.length; i++)
            for (int j = 0; j < tM[0].length; j++)
                tM[i][j] = M[j][i];
        return tM;
    }
}