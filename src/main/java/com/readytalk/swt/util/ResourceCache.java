package com.readytalk.swt.util;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Resource;

final class ResourceCache<K, R extends Resource> {

  private static final Logger LOG = Logger.getLogger(ResourceCache.class.getName());
  private static final long WAIT_TIME = 10L;

  long limit;
  long creationCount = 0;
  long disposedCount = 0;
  HashMap<K, ResourceReference> resourceMap;
  ReferenceQueue<R> referenceQueue;
  ResourceBuilder<K, R> builder;

  ResourceCache(final ResourceBuilder<K, R> builder, final long limit) {
    this.limit = limit;
    this.builder = builder;
    resourceMap = new HashMap<K, ResourceReference>();
    referenceQueue =  new ReferenceQueue<R>();
  }

  public R get(Device device, final K key) {
    R resource = null;
    ResourceReference reference = resourceMap.get(key);

    if (reference == null || (resource = reference.get()) == null) {
      resource = builder.build(device, key);
      LOG.log(Level.INFO, "ResourceCache created " + resource.toString() + " created: " + creationCount + " disposed:" + disposedCount);
      reference = new ResourceReference(key, resource, referenceQueue);
      resourceMap.put(key, reference);
      creationCount++;
    }

    purge();
    return resource;
  }

  void purge() {
    if (resourceMap.size() > limit) {
      System.gc();
    }

    Reference reference;
    try {

      while ((reference = referenceQueue.poll()) != null) {
        ResourceReference resourceReference = (ResourceReference)reference;
        resourceReference.cleanup();
        referenceQueue.remove(WAIT_TIME);
      }
    } catch (Exception e) {
      LOG.log(Level.INFO, e.getMessage(), e);
    }
  }

  void evacuate() {
    for (K k: resourceMap.keySet()) {
      ResourceReference resourceReference = resourceMap.remove(k);
      if (resourceReference != null) {
        resourceReference.cleanup();
      }
    }
  }

  int size() {
    return resourceMap.size();
  }

  private class ResourceReference extends PhantomReference<R> {
    private K key;

    public ResourceReference(K key, R resource, ReferenceQueue referenceQueue) {
      super(resource, referenceQueue);
      this.key = key;
    }

    @Override
    public R get() {
      try {
        // PhantomReference's get always returns null; while WeakReference is cleaned up too quickly
        Field field = Reference.class.getDeclaredField("referent");
        field.setAccessible(true);
        return (R)field.get(this);
      } catch (Exception e) {
        LOG.log(Level.SEVERE, e.getMessage(), e);
      }
      return null;
    }

    /*
     * Make sure no resource reference escapes the scope of this method!
     */
    public void cleanup() {
      R resource = get();
      if (resource != null && !resource.isDisposed()) {
        disposedCount++;
        LOG.log(Level.INFO, "ResourceCache disposing " + resource.toString() + " created: " + creationCount + " disposed:" + disposedCount);
        resource.dispose();
        resource = null;
        resourceMap.remove(key);
        clear();
      }
    }
  }
}
