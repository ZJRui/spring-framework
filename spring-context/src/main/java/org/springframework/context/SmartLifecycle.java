/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context;

/**
 * An extension of the {@link Lifecycle} interface for those objects that require
 * to be started upon {@code ApplicationContext} refresh and/or shutdown in a
 * particular order.
 *
 * <p>The {@link #isAutoStartup()} return value indicates whether this object should
 * be started at the time of a context refresh. The callback-accepting
 * {@link #stop(Runnable)} method is useful for objects that have an asynchronous
 * shutdown process. Any implementation of this interface <i>must</i> invoke the
 * callback's {@code run()} method upon shutdown completion to avoid unnecessary
 * delays in the overall {@code ApplicationContext} shutdown.
 *
 * <p>This interface extends {@link Phased}, and the {@link #getPhase()} method's
 * return value indicates the phase within which this {@code Lifecycle} component
 * should be started and stopped. The startup process begins with the <i>lowest</i>
 * phase value and ends with the <i>highest</i> phase value ({@code Integer.MIN_VALUE}
 * is the lowest possible, and {@code Integer.MAX_VALUE} is the highest possible).
 * The shutdown process will apply the reverse order. Any components with the
 * same value will be arbitrarily ordered within the same phase.
 *
 * <p>Example: if component B depends on component A having already started,
 * then component A should have a lower phase value than component B. During
 * the shutdown process, component B would be stopped before component A.
 *
 * <p>Any explicit "depends-on" relationship will take precedence over the phase
 * order such that the dependent bean always starts after its dependency and
 * always stops before its dependency.
 *
 * <p>Any {@code Lifecycle} components within the context that do not also
 * implement {@code SmartLifecycle} will be treated as if they have a phase
 * value of {@code 0}. This allows a {@code SmartLifecycle} component to start
 * before those {@code Lifecycle} components if the {@code SmartLifecycle}
 * component has a negative phase value, or the {@code SmartLifecycle} component
 * may start after those {@code Lifecycle} components if the {@code SmartLifecycle}
 * component has a positive phase value.
 *
 * <p>Note that, due to the auto-startup support in {@code SmartLifecycle}, a
 * {@code SmartLifecycle} bean instance will usually get initialized on startup
 * of the application context in any case. As a consequence, the bean definition
 * lazy-init flag has very limited actual effect on {@code SmartLifecycle} beans.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see LifecycleProcessor
 * @see ConfigurableApplicationContext
 */
public interface SmartLifecycle extends Lifecycle, Phased {
	/**
	 * SmartLifecycle 是一个接口。当Spring容器加载所有bean并完成初始化之后，
	 * 会接着回调实现该接口的类中对应的方法（start()方法）。
	 *
	 * Lifecycle 接口的扩展，用于那些需要在 ApplicationContext 以特定顺序刷新和/或关闭时启动的对象。
	 * isAutoStartup() 返回值指示是否应在上下文刷新时启动此对象。 接受回调的 stop(Runnable) 方法对于具有异步关闭过程的对象很有用。
	 * 此接口的任何实现都必须在关闭完成时调用回调的 run() 方法，以避免在整个 ApplicationContext 关闭过程中出现不必要的延迟。
	 * 该接口扩展了 Phased，getPhase() 方法的返回值指示该 Lifecycle 组件应启动和停止的阶段。 启动过程从最低的相位值开始，
	 * 到最高的相位值结束（Integer.MIN_VALUE 是最低的，Integer.MAX_VALUE 是最高的）。 关闭过程将应用相反的顺序。
	 * 具有相同值的任何组件将在同一相内任意排序。
	 * 示例：如果组件 B 依赖于组件 A 已经启动，则组件 A 的相位值应低于组件 B。在关闭过程中，组件 B 将先于组件 A 停止。
	 * 任何明确的“依赖”关系都将优先于阶段顺序，以便依赖 bean 始终在其依赖之后启动并始终在其依赖之前停止。
	 *
	 * 上下文中任何未实现 SmartLifecycle 的生命周期组件都将被视为相位值为 0。如果 SmartLifecycle 组件具有负相位值，
	 * 这允许 SmartLifecycle 组件在这些生命周期组件之前启动，或者 SmartLifecycle 组件 如果 SmartLifecycle 组件具有正相位值，
	 * 则可能会在这些 Lifecycle 组件之后启动。
	 * 请注意，由于 SmartLifecycle 中的自动启动支持，SmartLifecycle bean 实例通常会在任何情况下在应用程序上下文启动时进行初始化。
	 * 因此，bean 定义的 lazy-init 标志对 SmartLifecycle bean 的实际影响非常有限。
	 * 自从：
	 * 3.0
	 *
	 */

	/**
	 * The default phase for {@code SmartLifecycle}: {@code Integer.MAX_VALUE}.
	 * <p>This is different from the common phase {@code 0} associated with regular
	 * {@link Lifecycle} implementations, putting the typically auto-started
	 * {@code SmartLifecycle} beans into a later startup phase and an earlier
	 * shutdown phase.
	 * @since 5.1
	 * @see #getPhase()
	 * @see org.springframework.context.support.DefaultLifecycleProcessor#getPhase(Lifecycle)
	 */
	int DEFAULT_PHASE = Integer.MAX_VALUE;


	/**
	 * Returns {@code true} if this {@code Lifecycle} component should get
	 * started automatically by the container at the time that the containing
	 * {@link ApplicationContext} gets refreshed.
	 * <p>A value of {@code false} indicates that the component is intended to
	 * be started through an explicit {@link #start()} call instead, analogous
	 * to a plain {@link Lifecycle} implementation.
	 * <p>The default implementation returns {@code true}.
	 * @see #start()
	 * @see #getPhase()
	 * @see LifecycleProcessor#onRefresh()
	 * @see ConfigurableApplicationContext#refresh()
	 */
	default boolean isAutoStartup() {
		return true;
	}

	/**
	 * Indicates that a Lifecycle component must stop if it is currently running.
	 * <p>The provided callback is used by the {@link LifecycleProcessor} to support
	 * an ordered, and potentially concurrent, shutdown of all components having a
	 * common shutdown order value. The callback <b>must</b> be executed after
	 * the {@code SmartLifecycle} component does indeed stop.
	 * <p>The {@link LifecycleProcessor} will call <i>only</i> this variant of the
	 * {@code stop} method; i.e. {@link Lifecycle#stop()} will not be called for
	 * {@code SmartLifecycle} implementations unless explicitly delegated to within
	 * the implementation of this method.
	 * <p>The default implementation delegates to {@link #stop()} and immediately
	 * triggers the given callback in the calling thread. Note that there is no
	 * synchronization between the two, so custom implementations may at least
	 * want to put the same steps within their common lifecycle monitor (if any).
	 * @see #stop()
	 * @see #getPhase()
	 */
	default void stop(Runnable callback) {
		stop();
		callback.run();
	}

	/**
	 * Return the phase that this lifecycle object is supposed to run in.
	 * <p>The default implementation returns {@link #DEFAULT_PHASE} in order to
	 * let {@code stop()} callbacks execute after regular {@code Lifecycle}
	 * implementations.
	 * @see #isAutoStartup()
	 * @see #start()
	 * @see #stop(Runnable)
	 * @see org.springframework.context.support.DefaultLifecycleProcessor#getPhase(Lifecycle)
	 */
	@Override
	default int getPhase() {
		return DEFAULT_PHASE;
	}

}
