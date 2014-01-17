package com.readytalk.swt.util;

import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Resource;

interface ResourceBuilder<K, R extends Resource> {
  R build(Device device, K key);
}
