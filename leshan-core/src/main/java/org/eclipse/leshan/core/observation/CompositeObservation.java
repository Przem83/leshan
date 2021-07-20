package org.eclipse.leshan.core.observation;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;

import java.util.*;

/**
 * An composite-observation of a resource provided by a LWM2M Client.
 */
public class CompositeObservation extends Observation {

    private final List<LwM2mPath> paths;

    public CompositeObservation(byte[] id, String registrationId, List<LwM2mPath> paths, ContentFormat contentFormat,
                                Map<String, String> context) {
        super(id, registrationId, contentFormat, context);
        this.paths = paths;
    }

    public List<LwM2mPath> getPaths() {
        return paths;
    }

    @Override
    public String toString() {
        return "CompositeObservation{" +
                "paths=" + paths +
                ", id=" + Arrays.toString(id) +
                ", contentFormat=" + contentFormat +
                ", registrationId='" + registrationId + '\'' +
                ", context=" + context +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompositeObservation)) return false;
        if (!super.equals(o)) return false;
        CompositeObservation that = (CompositeObservation) o;
        return Objects.equals(paths, that.paths);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), paths);
    }
}
