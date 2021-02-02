/**
 * Copyright 2018 The original authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
**/

package io.dekorate.crd.decorator;

import java.util.concurrent.atomic.AtomicReference;

import io.dekorate.kubernetes.decorator.Decorator;
import io.fabric8.kubernetes.api.builder.Predicate;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionSpecFluent;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionVersion;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionVersionBuilder;

public class EnsureSingleStorageVersionDecorator extends CustomResourceDefinitionDecorator<CustomResourceDefinitionSpecFluent<?>> {

  private final AtomicReference<String> storageVersion = new AtomicReference<>();

	public EnsureSingleStorageVersionDecorator(String name) {
		super(name);
	}

	@Override
	public void andThenVisit(CustomResourceDefinitionSpecFluent<?> spec, ObjectMeta resourceMeta) {
    Predicate<CustomResourceDefinitionVersionBuilder> hasStorageVersion = new Predicate<CustomResourceDefinitionVersionBuilder>()  {
        @Override
        public Boolean apply(CustomResourceDefinitionVersionBuilder version) {
          return version.isStorage();
        }
    };

    if (spec.hasVersions() && !spec.hasMatchingVersion(hasStorageVersion)) {
      System.out.println("Only one version found... Setting to storage!");
      spec.editFirstVersion().withStorage(true).endVersion();
    }
                                                                             
    for (CustomResourceDefinitionVersion version : spec.buildVersions()) {
      if (version.getStorage()) {
        String existing = storageVersion.get();
        if (existing != null && !existing.equals(version.getName())) {
          throw new IllegalStateException(String.format("Version %s is marked as storage and so is %s. Only one version should be marked as storage per custom resource.", version.getName(), existing));
        } else {
          storageVersion.set(version.getName());
        }
      }
    }
	}

	@Override
	public Class<? extends Decorator>[] after() {
    return new Class[] { AddCustomResourceDefinitionResourceDecorator.class, AddCustomResourceDefintionVersionDecorator.class, SetStorageVersionDecorator.class };
	}
}
