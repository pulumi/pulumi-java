package io.pulumi.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static java.util.Objects.requireNonNull;

@ParametersAreNonnullByDefault
public abstract class GSONObject<T extends JsonElement> {

   protected final T node;
   private final Gson gson;

   protected GSONObject(T node) {
      this.node = requireNonNull(node);
      this.gson = new GsonBuilder().serializeNulls().create();
   }

   public int hashCode() {
      return this.node.hashCode();
   }

   @Override
   public boolean equals(@Nullable Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      GSONObject<?> that = (GSONObject<?>) o;
      return node.equals(that.node);
   }

   public String asString() {
      return gson.toJson(this.node);
   }

   public String toString() {
      return String.format("%s(%s)", this.getClass().getSimpleName(), this.asString());
   }
}