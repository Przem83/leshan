package org.eclipse.leshan.client.util;

import org.eclipse.leshan.core.node.*;

import java.util.*;
import java.util.stream.Collectors;

public class DuplicateNodeHelper {
    public static Map<LwM2mPath, LwM2mNode> removeDuplicateNodes(Map<LwM2mPath, LwM2mNode> content) {
        Map<LwM2mObjectInstance, Long> instances = content.values().stream()
                .map(DuplicateNodeHelper::breakDownToInstances).flatMap(Collection::stream)
                .collect(Collectors.groupingBy(instance -> instance, Collectors.counting()));

        Map<LwM2mResource, Long> resources = content.values().stream().map(DuplicateNodeHelper::breakDownToResources)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(resource -> resource, Collectors.counting()));

        return content.entrySet().stream().filter(entry -> {
            LwM2mNode node = entry.getValue();
            return (resources.get(node) == null || resources.get(node) == 1) &&
                    ((instances.get(node) == null || instances.get(node) == 1));
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static List<LwM2mResource> breakDownToResources(LwM2mNode node) {
        if (node instanceof LwM2mObject) {
            return breakDownToInstances(node).stream().map(DuplicateNodeHelper::breakDownToResources)
                    .flatMap(Collection::stream).collect(Collectors.toList());
        } else if (node instanceof LwM2mObjectInstance) {
            return new ArrayList<>(((LwM2mObjectInstance) node).getResources().values());
        } else if (node instanceof LwM2mResource) {
            return Collections.singletonList((LwM2mResource) node);
        } else {
            return Collections.emptyList();
        }
    }

    private static List<LwM2mObjectInstance> breakDownToInstances(LwM2mNode node) {
        if (node instanceof LwM2mObject) {
            return new ArrayList<>(((LwM2mObject) node).getInstances().values());
        } else if (node instanceof LwM2mObjectInstance) {
            return Collections.singletonList((LwM2mObjectInstance) node);
        } else {
            return Collections.emptyList();
        }
    }
}
