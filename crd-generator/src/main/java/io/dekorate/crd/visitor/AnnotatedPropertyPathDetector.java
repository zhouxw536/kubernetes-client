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

package io.dekorate.crd.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import io.sundr.builder.Predicate;
import io.sundr.builder.TypedVisitor;
import io.sundr.codegen.model.AnnotationRefBuilder;
import io.sundr.codegen.model.Property;
import io.sundr.codegen.model.ClassRef;
import io.sundr.codegen.model.TypeDef;
import io.sundr.codegen.model.TypeDefBuilder;
import io.sundr.codegen.utils.TypeUtils;

public class AnnotatedPropertyPathDetector extends TypedVisitor<TypeDefBuilder> {

  protected static final String DOT = ".";
  protected static final String STATUS = ".status.";

  private final String prefix;
  private final String annotationName;
  private final List<String> parents;
  private final AtomicReference<Optional<String>> reference;

  private final Predicate<AnnotationRefBuilder> predicate = new Predicate<>() {
        @Override
        public boolean apply(AnnotationRefBuilder ref) {
         return ref.getClassRef().getName().equals(annotationName);
        }
    };
 
  public AnnotatedPropertyPathDetector(String prefix, String annotationName) {
    this(prefix, annotationName, new ArrayList<>());
  }

  public AnnotatedPropertyPathDetector(String prefix, String annotationName, List<String> parents) {
    this(prefix, annotationName, parents, new AtomicReference<>(Optional.empty()));
  }

  public AnnotatedPropertyPathDetector(String prefix, String annotationName, List<String> parents, AtomicReference<Optional<String>> reference) {
    this.prefix = prefix;
    this.annotationName = annotationName;
    this.parents = parents;
    this.reference = reference;
  }

  @Override
  public void visit(TypeDefBuilder builder) {
    TypeDef type = builder.build();
    for (Property p : TypeUtils.allProperties(type)) {
        List<String> newParents = new ArrayList<>(parents);
        boolean match = p.getAnnotations().stream().anyMatch(a -> a.getClassRef().getName().equals(annotationName));
        if (match) {
          newParents.add(p.getName());
          reference.set(Optional.of(newParents.stream().collect(Collectors.joining(DOT, prefix, ""))));
          return;
        }
    }

    TypeUtils.allProperties(type).stream().filter(p -> p.getTypeRef() instanceof ClassRef).forEach(p -> {
        ClassRef classRef = (ClassRef) p.getTypeRef();
        TypeDef propertyType = classRef.getDefinition();
        List<String> newParents = new ArrayList<>(parents);
        newParents.add(p.getName());
        new TypeDefBuilder(propertyType)
          .accept(new AnnotatedPropertyPathDetector(prefix, annotationName, newParents, reference))
          .build();
      });
  }

  public Optional<String> getPath() {
    return reference.get();
  }
}
