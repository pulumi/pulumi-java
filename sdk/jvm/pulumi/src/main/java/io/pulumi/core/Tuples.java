package io.pulumi.core;

import com.google.common.base.MoreObjects;

import java.util.Objects;

// TODO: consider making this class a package, would it make it a better API?
public class Tuples {

    public interface Tuple {
        // Empty

        // TODO: consider moving TupleX factory methods here to get better usage: Tuple.of, instead of Tuples.of
    }

    public static final class Tuple0 implements Tuple {
        public static final Tuple0 Empty = new Tuple0();

        private Tuple0() {
            // Empty
        }

        public <T1> Tuple1<T1> append(T1 t1) {
            return new Tuple1<>(t1);
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

    public static final class Tuple1<T1> implements Tuple {
        public final T1 t1;

        public Tuple1(T1 t1) {
            this.t1 = Objects.requireNonNull(t1);
        }

        public <T2> Tuple2<T1, T2> append(T2 t2) {
            return new Tuple2<>(this.t1, t2);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("t1", t1)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple1<?> tuple1 = (Tuple1<?>) o;
            return Objects.equals(t1, tuple1.t1);
        }

        @Override
        public int hashCode() {
            return Objects.hash(t1);
        }
    }

    public static final class Tuple2<T1, T2> implements Tuple {
        public final T1 t1;
        public final T2 t2;

        public Tuple2(T1 t1, T2 t2) {
            this.t1 = Objects.requireNonNull(t1);
            this.t2 = Objects.requireNonNull(t2);
        }

        public <T3> Tuple3<T1, T2, T3> append(T3 t3) {
            return new Tuple3<>(this.t1, this.t2, t3);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("t1", t1)
                    .add("t2", t2)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple2<?, ?> tuple2 = (Tuple2<?, ?>) o;
            return Objects.equals(t1, tuple2.t1) && Objects.equals(t2, tuple2.t2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(t1, t2);
        }
    }

    public static <T1, T2> Tuple2<T1, T2> of(T1 t1, T2 t2) {
        return new Tuple2<>(t1, t2);
    }

    public static final class Tuple3<T1, T2, T3> implements Tuple {
        public final T1 t1;
        public final T2 t2;
        public final T3 t3;

        public Tuple3(T1 t1, T2 t2, T3 t3) {
            this.t1 = Objects.requireNonNull(t1);
            this.t2 = Objects.requireNonNull(t2);
            this.t3 = Objects.requireNonNull(t3);
        }

        public <T4> Tuple4<T1, T2, T3, T4> append(T4 t4) {
            return new Tuple4<>(this.t1, this.t2, this.t3, t4);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("t1", t1)
                    .add("t2", t2)
                    .add("t3", t3)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple3<?, ?, ?> tuple3 = (Tuple3<?, ?, ?>) o;
            return Objects.equals(t1, tuple3.t1)
                    && Objects.equals(t2, tuple3.t2)
                    && Objects.equals(t3, tuple3.t3);
        }

        @Override
        public int hashCode() {
            return Objects.hash(t1, t2, t3);
        }
    }

    public static <T1, T2, T3> Tuple3<T1, T2, T3> of(T1 t1, T2 t2, T3 t3) {
        return new Tuple3<>(t1, t2, t3);
    }

    public static final class Tuple4<T1, T2, T3, T4> implements Tuple {
        public final T1 t1;
        public final T2 t2;
        public final T3 t3;
        public final T4 t4;

        public Tuple4(T1 t1, T2 t2, T3 t3, T4 t4) {
            this.t1 = Objects.requireNonNull(t1);
            this.t2 = Objects.requireNonNull(t2);
            this.t3 = Objects.requireNonNull(t3);
            this.t4 = Objects.requireNonNull(t4);
        }

        public <T5> Tuple5<T1, T2, T3, T4, T5> append(T5 t5) {
            return new Tuple5<>(this.t1, this.t2, this.t3, this.t4, t5);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("t1", t1)
                    .add("t2", t2)
                    .add("t3", t3)
                    .add("t4", t4)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple4<?, ?, ?, ?> tuple4 = (Tuple4<?, ?, ?, ?>) o;
            return Objects.equals(t1, tuple4.t1)
                    && Objects.equals(t2, tuple4.t2)
                    && Objects.equals(t3, tuple4.t3)
                    && Objects.equals(t4, tuple4.t4);
        }

        @Override
        public int hashCode() {
            return Objects.hash(t1, t2, t3, t4);
        }
    }

    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> of(T1 t1, T2 t2, T3 t3, T4 t4) {
        return new Tuple4<>(t1, t2, t3, t4);
    }

    public static final class Tuple5<T1, T2, T3, T4, T5> implements Tuple {
        public final T1 t1;
        public final T2 t2;
        public final T3 t3;
        public final T4 t4;
        public final T5 t5;

        public Tuple5(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
            this.t1 = Objects.requireNonNull(t1);
            this.t2 = Objects.requireNonNull(t2);
            this.t3 = Objects.requireNonNull(t3);
            this.t4 = Objects.requireNonNull(t4);
            this.t5 = Objects.requireNonNull(t5);
        }

        public <T6> Tuple6<T1, T2, T3, T4, T5, T6> append(T6 t6) {
            return new Tuple6<>(this.t1, this.t2, this.t3, this.t4, this.t5, t6);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("t1", t1)
                    .add("t2", t2)
                    .add("t3", t3)
                    .add("t4", t4)
                    .add("t5", t5)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple5<?, ?, ?, ?, ?> tuple5 = (Tuple5<?, ?, ?, ?, ?>) o;
            return Objects.equals(t1, tuple5.t1)
                    && Objects.equals(t2, tuple5.t2)
                    && Objects.equals(t3, tuple5.t3)
                    && Objects.equals(t4, tuple5.t4)
                    && Objects.equals(t5, tuple5.t5);
        }

        @Override
        public int hashCode() {
            return Objects.hash(t1, t2, t3, t4, t5);
        }
    }

    public static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> of(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        return new Tuple5<>(t1, t2, t3, t4, t5);
    }

    public static final class Tuple6<T1, T2, T3, T4, T5, T6> implements Tuple {
        public final T1 t1;
        public final T2 t2;
        public final T3 t3;
        public final T4 t4;
        public final T5 t5;
        public final T6 t6;

        public Tuple6(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
            this.t1 = Objects.requireNonNull(t1);
            this.t2 = Objects.requireNonNull(t2);
            this.t3 = Objects.requireNonNull(t3);
            this.t4 = Objects.requireNonNull(t4);
            this.t5 = Objects.requireNonNull(t5);
            this.t6 = Objects.requireNonNull(t6);
        }

        public <T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> append(T7 t7) {
            return new Tuple7<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, t7);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("t1", t1)
                    .add("t2", t2)
                    .add("t3", t3)
                    .add("t4", t4)
                    .add("t5", t5)
                    .add("t6", t6)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple6<?, ?, ?, ?, ?, ?> tuple6 = (Tuple6<?, ?, ?, ?, ?, ?>) o;
            return Objects.equals(t1, tuple6.t1)
                    && Objects.equals(t2, tuple6.t2)
                    && Objects.equals(t3, tuple6.t3)
                    && Objects.equals(t4, tuple6.t4)
                    && Objects.equals(t5, tuple6.t5)
                    && Objects.equals(t6, tuple6.t6);
        }

        @Override
        public int hashCode() {
            return Objects.hash(t1, t2, t3, t4, t5, t6);
        }
    }

    public static <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> of(
            T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        return new Tuple6<>(t1, t2, t3, t4, t5, t6);
    }

    public static final class Tuple7<T1, T2, T3, T4, T5, T6, T7> implements Tuple {
        public final T1 t1;
        public final T2 t2;
        public final T3 t3;
        public final T4 t4;
        public final T5 t5;
        public final T6 t6;
        public final T7 t7;

        public Tuple7(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) {
            this.t1 = Objects.requireNonNull(t1);
            this.t2 = Objects.requireNonNull(t2);
            this.t3 = Objects.requireNonNull(t3);
            this.t4 = Objects.requireNonNull(t4);
            this.t5 = Objects.requireNonNull(t5);
            this.t6 = Objects.requireNonNull(t6);
            this.t7 = Objects.requireNonNull(t7);
        }

        public <T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> append(T8 t8) {
            return new Tuple8<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, t8);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("t1", t1)
                    .add("t2", t2)
                    .add("t3", t3)
                    .add("t4", t4)
                    .add("t5", t5)
                    .add("t6", t6)
                    .add("t7", t7)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple7<?, ?, ?, ?, ?, ?, ?> tuple7 = (Tuple7<?, ?, ?, ?, ?, ?, ?>) o;
            return Objects.equals(t1, tuple7.t1)
                    && Objects.equals(t2, tuple7.t2)
                    && Objects.equals(t3, tuple7.t3)
                    && Objects.equals(t4, tuple7.t4)
                    && Objects.equals(t5, tuple7.t5)
                    && Objects.equals(t6, tuple7.t6)
                    && Objects.equals(t7, tuple7.t7);
        }

        @Override
        public int hashCode() {
            return Objects.hash(t1, t2, t3, t4, t5, t6, t7);
        }
    }

    public static <T1, T2, T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> of(
            T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) {
        return new Tuple7<>(t1, t2, t3, t4, t5, t6, t7);
    }

    public static final class Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> implements Tuple {
        public final T1 t1;
        public final T2 t2;
        public final T3 t3;
        public final T4 t4;
        public final T5 t5;
        public final T6 t6;
        public final T7 t7;
        public final T8 t8;

        public Tuple8(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) {
            this.t1 = Objects.requireNonNull(t1, "Expected non-null t1");
            this.t2 = Objects.requireNonNull(t2, "Expected non-null t2");
            this.t3 = Objects.requireNonNull(t3, "Expected non-null t3");
            this.t4 = Objects.requireNonNull(t4, "Expected non-null t4");
            this.t5 = Objects.requireNonNull(t5, "Expected non-null t5");
            this.t6 = Objects.requireNonNull(t6, "Expected non-null t6");
            this.t7 = Objects.requireNonNull(t7, "Expected non-null t7");
            this.t8 = Objects.requireNonNull(t8, "Expected non-null t8");
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("t1", t1)
                    .add("t2", t2)
                    .add("t3", t3)
                    .add("t4", t4)
                    .add("t5", t5)
                    .add("t6", t6)
                    .add("t7", t7)
                    .add("t8", t8)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple8<?, ?, ?, ?, ?, ?, ?, ?> tuple8 = (Tuple8<?, ?, ?, ?, ?, ?, ?, ?>) o;
            return Objects.equals(t1, tuple8.t1)
                    && Objects.equals(t2, tuple8.t2)
                    && Objects.equals(t3, tuple8.t3)
                    && Objects.equals(t4, tuple8.t4)
                    && Objects.equals(t5, tuple8.t5)
                    && Objects.equals(t6, tuple8.t6)
                    && Objects.equals(t7, tuple8.t7)
                    && Objects.equals(t8, tuple8.t8);
        }

        @Override
        public int hashCode() {
            return Objects.hash(t1, t2, t3, t4, t5, t6, t7, t8);
        }
    }

    public static <T1, T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> of(
            T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) {
        return new Tuple8<>(t1, t2, t3, t4, t5, t6, t7, t8);
    }
}
