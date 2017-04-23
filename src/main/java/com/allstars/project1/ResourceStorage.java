package com.allstars.project1;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Jack on 23/3/2017.
 */
public class ResourceStorage {

    Map<ResourceKey,Resource> resources = Collections.synchronizedMap(new HashMap<>());

    public synchronized HashSet<Resource> searchWithTemplate(Resource template) {
        return new HashSet<>(resources.values().stream().filter(
                (Resource r) -> r.matchesTemplate(template)
        ).collect(Collectors.toSet()));
    }

    public synchronized Set<String> getUriSet() {
        return new HashSet<>(resources.values().stream().map(Resource::getUri).collect(Collectors.toSet()));
    }

    public synchronized void add(Resource resource) {
        resources.put(resource.getKey(), resource);
        Logging.logFine("Added new resource");
        Logging.logFine(this.toJson());
    }

    public synchronized void remove(Resource resource) {
        resources.remove(resource.getKey());
        Logging.logFine("Removed resource");
        Logging.logFine(this.toJson());
    }

    public synchronized boolean containsKey(Resource r){
        return resources.containsKey(r.getKey());
    }

    // for debug usage
    public synchronized String toJson() {
        return Static.GSON.toJson(resources.values().toArray(new Resource[this.resources.size()]));
    }
}
