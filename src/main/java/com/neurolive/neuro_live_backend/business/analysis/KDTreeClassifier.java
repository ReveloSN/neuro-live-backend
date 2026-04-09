package com.neurolive.neuro_live_backend.business.analysis;

import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
// Implementa un clasificador inicial basado en busqueda del vecino mas cercano.
public class KDTreeClassifier {

    private static final List<LabeledPoint> TRAINING_POINTS = List.of(
            new LabeledPoint(new float[]{2.0f, 0.5f, 0.02f, 95.0f, 110.0f}, StateEnum.NORMAL, "stable"),
            new LabeledPoint(new float[]{10.0f, 2.0f, 0.10f, 150.0f, 180.0f}, StateEnum.RISK_ELEVATED, "elevated"),
            new LabeledPoint(new float[]{16.0f, 3.0f, 0.18f, 190.0f, 240.0f}, StateEnum.RISK_ELEVATED, "keystroke-risk"),
            new LabeledPoint(new float[]{28.0f, 5.0f, 0.26f, 280.0f, 330.0f}, StateEnum.ACTIVE_CRISIS, "acute"),
            new LabeledPoint(new float[]{35.0f, 7.0f, 0.32f, 320.0f, 380.0f}, StateEnum.ACTIVE_CRISIS, "critical")
    );

    private final Node root;

    public KDTreeClassifier() {
        this.root = build(TRAINING_POINTS, 0);
    }

    public ClassificationResult classify(CrisisFeatureVector featureVector) {
        if (featureVector == null) {
            throw new IllegalArgumentException("Feature vector is required");
        }

        NearestMatch nearestMatch = findNearest(root, featureVector.toArray(), 0, null);
        if (nearestMatch == null) {
            throw new IllegalStateException("No training points were loaded for crisis classification");
        }

        return new ClassificationResult(
                nearestMatch.point().label(),
                nearestMatch.distance(),
                nearestMatch.point().name()
        );
    }

    private Node build(List<LabeledPoint> points, int depth) {
        if (points == null || points.isEmpty()) {
            return null;
        }

        int axis = depth % points.getFirst().coordinates().length;
        List<LabeledPoint> sorted = points.stream()
                .sorted(Comparator.comparingDouble(point -> point.coordinates()[axis]))
                .toList();
        int medianIndex = sorted.size() / 2;

        return new Node(
                sorted.get(medianIndex),
                axis,
                build(sorted.subList(0, medianIndex), depth + 1),
                build(sorted.subList(medianIndex + 1, sorted.size()), depth + 1)
        );
    }

    private NearestMatch findNearest(Node node, float[] target, int depth, NearestMatch currentBest) {
        if (node == null) {
            return currentBest;
        }

        float distance = squaredDistance(node.point().coordinates(), target);
        NearestMatch best = currentBest;
        if (best == null || distance < best.distance()) {
            best = new NearestMatch(node.point(), distance);
        }

        int axis = depth % target.length;
        Node nearBranch = target[axis] < node.point().coordinates()[axis] ? node.left() : node.right();
        Node farBranch = nearBranch == node.left() ? node.right() : node.left();

        best = findNearest(nearBranch, target, depth + 1, best);

        float axisDistance = target[axis] - node.point().coordinates()[axis];
        if (best == null || axisDistance * axisDistance < best.distance()) {
            best = findNearest(farBranch, target, depth + 1, best);
        }

        return best;
    }

    private float squaredDistance(float[] left, float[] right) {
        float total = 0.0f;
        for (int index = 0; index < left.length; index++) {
            float delta = left[index] - right[index];
            total += delta * delta;
        }
        return total;
    }

    public record ClassificationResult(StateEnum state, float squaredDistance, String matchedProfile) {
    }

    private record LabeledPoint(float[] coordinates, StateEnum label, String name) {
    }

    private record Node(LabeledPoint point, int axis, Node left, Node right) {
    }

    private record NearestMatch(LabeledPoint point, float distance) {
    }
}
