package frc.team_8840_lib.AI;


import frc.team_8840_lib.utils.math.MathUtils;
import frc.team_8840_lib.utils.math.Matrix;

import java.util.List;

public class NeuralNetwork {
    private final int numberOfInputNodes;
    private final int numberOfHiddenNodes;
    private final int numberOfOutputNodes;

    private final int numberOfHiddenLayers;

    private final double learningRate;


    private Matrix[] weights;
    private Matrix[] biases;

    public NeuralNetwork(int numberOfInputNodes, int numberOfHiddenNodes, int numberOfOutputNodes, int numberOfHiddenLayers, double learningRate) {
        this.numberOfInputNodes = numberOfInputNodes;
        this.numberOfHiddenNodes = numberOfHiddenNodes;
        this.numberOfOutputNodes = numberOfOutputNodes;
        this.numberOfHiddenLayers = numberOfHiddenLayers;
        this.learningRate = learningRate;

        /* Small Diagram to check if my old code is insane or not. I:nput, H:idden, O:utput, B:ias, W:eight *//*
        I \
        I - H1 - H2 \
        I - H1 - H2 - O

        I3 W6 B2 W4 B2 W2 B1
        * */// Checks out.

        this.weights = new Matrix[numberOfHiddenLayers + 1];
        //Last weight is to the output layer
        this.biases = new Matrix[numberOfHiddenLayers + 1];
        //Last layer is bias for output layer

        for (int weightLayer = 0; weightLayer < numberOfHiddenLayers; weightLayer++) {
            this.weights[weightLayer] = new Matrix(numberOfHiddenNodes, numberOfInputNodes);
        }

        for (int biasLayer = 0; biasLayer < numberOfHiddenLayers + 1; biasLayer++) {
            this.biases[biasLayer] = new Matrix(numberOfHiddenNodes, 1);
        }
    }

    public Matrix predictForMatrix(double[] inputs) {
        Matrix inputsMatrix = Matrix.fromArray(inputs);
        Matrix[] outputs = new Matrix[numberOfHiddenLayers + 1];
        outputs[0] = inputsMatrix;

        for (int i = 0; i < numberOfHiddenLayers; i++) {
            outputs[i + 1] = outputs[i].multiply(weights[i]).add(biases[i]);
            outputs[i + 1].map(MathUtils::sigmoid);
        }

        return outputs[numberOfHiddenLayers];
    }

    public Double[] predict(double[] inputs) {
        Matrix outputs = predictForMatrix(inputs);
        List<Double> outputList = outputs.toArray();
        return outputList.toArray(new Double[outputList.size()]);
    }

    public void calculateAndUpdateError(double[] inputs, double[] expectedOutputs) {
        Matrix inputsMatrix = Matrix.fromArray(inputs);
        Matrix expectedOutputsMatrix = Matrix.fromArray(expectedOutputs);

        Matrix[] outputs = new Matrix[numberOfHiddenLayers + 1];
        outputs[0] = inputsMatrix;

        for (int i = 0; i < numberOfHiddenLayers; i++) {
            outputs[i + 1] = outputs[i].multiply(weights[i]).add(biases[i]);
            outputs[i + 1].map(MathUtils::sigmoid);
        }

        Matrix error = outputs[numberOfHiddenLayers].subtract(expectedOutputsMatrix);

        for (int i = numberOfHiddenLayers; i >= 0; i--) {
            Matrix delta = outputs[i].multiply(error.map(MathUtils::sigmoidDerivative));
            Matrix gradient = delta.multiply(learningRate);
            biases[i] = biases[i].add(gradient);
            weights[i] = weights[i].add(outputs[i].transpose().multiply(gradient));
            error = weights[i].transpose().multiply(delta);
        }
    }

    private int[] generateRandomOrder(int length) {
        int[] order = new int[length];
        for (int i = 0; i < length; i++) {
            order[i] = i;
        }
        for (int i = 0; i < length; i++) {
            int randomIndex = (int) (Math.random() * length);
            int temp = order[i];
            order[i] = order[randomIndex];
            order[randomIndex] = temp;
        }
        return order;
    }

    public void train(double[][] inputs, double[][] expectedOutputs, int numberOfIterations) {
        for (int i = 0; i < numberOfIterations; i++) {
            int[] order = generateRandomOrder(inputs.length);
            for (int j = 0; j < order.length; j++) {
                calculateAndUpdateError(inputs[order[j]], expectedOutputs[j]);
            }
        }

        System.out.println("Finished Training.");
    }
}
