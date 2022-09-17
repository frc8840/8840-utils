package frc.team_8840_lib.utils.math;

import frc.team_8840_lib.utils.interfaces.MathMap;

import java.util.ArrayList;
import java.util.List;

public class Matrix {
    private double[][] data;
    private int rows, cols;

    /**
     * Get the number of rows in the matrix
     * @return the number of rows in the matrix
     * */
    public int getRows() {
        return rows;
    }

    /**
     * Get the number of columns in the matrix
     * @return the number of columns in the matrix
     * */
    public int getCols() {
        return cols;
    }

    /**
     * Get the data in the matrix as a list of lists
     * @return the data in the matrix as a list of lists
     * */
    public double[][] getData() {
        return data;
    }

    /**
     * Get a specific value in the matrix
     * @param i the row of the value
     *          0 <= i < rows
     * @param j the column of the value
     *          0 <= j < cols
     * @return the value at the specified row and column
     * */
    public double get(int i, int j) {
        return data[i][j];
    }

    /**
     * Sets the value at the specified row and column
     * @param i the row of the value
     *          0 <= i < rows
     * @param j the column of the value
     *          0 <= j < cols
     * @param value the value to set
     * */
    public void set(int i, int j, double value) {
        this.data[i][j] = value;
    }

    /**
     * Creates a new matrix with the specified dimensions. All values are set to a random value between -1 and 1
     * @param rows the number of rows in the matrix
     * @param cols the number of columns in the matrix
     * */
    public Matrix(int rows,int cols)
    {
        data = new double[rows][cols];
        this.rows = rows;
        this.cols = cols;

        this.initialize();
    }

    public Matrix(int rows, int cols, boolean zeros) {
        data = new double[rows][cols];
        this.rows = rows;
        this.cols = cols;

        if (!zeros) {
            this.initialize();
        } else this.initializeZeros();
    }

    /**
     * Initialize the matrix.
     * */
    public void initialize() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] = Math.random() * 2 - 1;
            }
        }
    }

    /**
     * Initialize the matrix with zeros.
     * */
    public void initializeZeros() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] = 0;
            }
        }
    }

    /**
     * Add a scalar to each number.
     * @param scalar the scalar to add
     * */
    public Matrix add(double scalar) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] += scalar;
            }
        }

        return this;
    }

    /**
     * Add a matrix to this matrix.
     * @param matrix the matrix to add
     * */
    public Matrix add(Matrix matrix) {
        if (matrix.getCols() != cols || matrix.getRows() != rows) {
            throw new IllegalArgumentException("Matrix dimensions do not match");
        }

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] += matrix.get(i, j);
            }
        }

        return this;
    }

    /**
     * Multiply this matrix by another matrix.
     * @param a the matrix to multiply by
     * */
    public Matrix multiply(Matrix a) {
        for(int i = 0; i < a.rows; i++) {
            for(int j = 0; j < a.cols; j++) {
                this.data[i][j] *= a.data[i][j];
            }
        }

        return this;
    }

    /**
     * Multiply this matrix by a scalar.
     * @param a the scalar to multiply by
     * */
    public Matrix multiply(double a) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                this.data[i][j] *= a;
            }
        }

        return this;
    }

    /**
     * Map the matrix using a function.
     * */
    public Matrix map(MathMap map) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] = map.map(data[i][j]);
            }
        }

        return this;
    }

    /**
     * Transpose the matrix.
     * */
    public Matrix transpose() {
        Matrix transpose = new Matrix(cols, rows);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                transpose.data[j][i] = data[i][j];
            }
        }

        return transpose;
    }

    /**
     * Subtract a scalar from each number.
     */
    public Matrix subtract(double scalar) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] -= scalar;
            }
        }

        return this;
    }

    /**
     * Subtract a matrix from this matrix.
     * @param matrix the matrix to subtract
     * */
    public Matrix subtract(Matrix matrix) {
        if (matrix.getCols() != cols || matrix.getRows() != rows) {
            throw new IllegalArgumentException("Matrix dimensions do not match");
        }

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] -= matrix.get(i, j);
            }
        }

        return this;
    }

    /**
     * Subtracts a matrix from another matrix
     * @param a the matrix to subtract from
     * @param b the matrix to subtract
     * @return the difference of the two matrices
     * */
    public static Matrix subtract(Matrix a, Matrix b) {
        if (a.getCols() != b.getCols() || a.getRows() != b.getCols()) {
            throw new IllegalArgumentException("Matrix dimensions do not match");
        }

        Matrix temp = new Matrix(a.getRows(), a.getCols());

        for (int i = 0; i < a.getRows(); i++) {
            for (int j = 0; j < a.getCols(); j++) {
                temp.set(i,j,a.get(i, j) - b.get(i, j));
            }
        }

        return temp;
    }

    /**
     * Copy a matrix, but reverse the order of the rows and columns.
     * @param a the matrix to copy
     * @return the transposed matrix
     * */
    public static Matrix transpose(Matrix a) {
        Matrix temp = new Matrix(a.getCols(), a.getRows());

        for (int i = 0; i < a.rows; i++) {
            for(int j = 0; j < a.cols; j++) {
                temp.set(j, i, a.data[i][j]);
            }
        }

        return temp;
    }

    /**
     * Multiply two matrices together.
     * @param a the first matrix
     * @param b the second matrix
     * @return the product of the two matrices
     * */
    public static Matrix multiply(Matrix a, Matrix b) {
        Matrix temp = new Matrix(a.rows,b.cols);

        for (int i = 0; i < temp.rows; i++) {
            for (int j = 0; j < temp.cols; j++) {
                double sum = 0;
                for (int k = 0; k < a.cols; k++) {
                    sum += a.data[i][k] * b.data[k][j];
                }
                temp.data[i][j] = sum;
            }
        }

        return temp;
    }

    /**
     * Converts an array to an X by 1 matrix.
     * @param x the array to convert
     * @return the converted matrix
     * */
    public static Matrix fromArray(double[] x) {
        Matrix temp = new Matrix(x.length,1);
        for (int i = 0; i < x.length; i++) temp.set(i, 0, x[i]);
        return temp;
    }

    /**
     * Converts this matrix to an array.
     * @reutrn the array
     * */
    public List<Double> toArray() {
        List<Double> temp = new ArrayList<Double>();

        for(int i = 0; i < rows; i++) {
            for(int j = 0; j < cols; j++) {
                temp.add(data[i][j]);
            }
        }

        return temp;
    }
}