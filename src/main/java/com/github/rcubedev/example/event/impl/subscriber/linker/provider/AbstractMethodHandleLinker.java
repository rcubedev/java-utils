package com.github.rcubedev.example.event.impl.subscriber.linker.provider;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.impl.subscriber.BindingFactory;
import com.github.rcubedev.example.event.impl.subscriber.linker.LinkerEngine;
import com.github.rcubedev.example.event.impl.subscriber.linker.LinkageContext;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.MemberAccessException;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.ModuleAccessException;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.StructuralLinkageException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public abstract class AbstractMethodHandleLinker implements LinkerEngine {

    @Override
    public final <T extends Event> BindingFactory<T> linkStrong(LinkageContext<T> context) throws
            StructuralLinkageException, ModuleAccessException, MemberAccessException {
        UnreflectedContext unreflected = prepareContext(context);
        return linkStrong(context, unreflected);
    }

    @Override
    public final <T extends Event> BindingFactory<T> linkWeak(LinkageContext<T> context) throws
            StructuralLinkageException, ModuleAccessException, MemberAccessException {
        UnreflectedContext unreflected = prepareContext(context);
        return linkWeak(context, unreflected);
    }

    protected abstract <T extends Event> BindingFactory<T> linkStrong(
            LinkageContext<T> context, UnreflectedContext unreflected) throws StructuralLinkageException;

    protected abstract <T extends Event> BindingFactory<T> linkWeak(
            LinkageContext<T> context, UnreflectedContext unreflected) throws StructuralLinkageException;

    private <T extends Event> UnreflectedContext prepareContext(LinkageContext<T> context) throws
            ModuleAccessException, MemberAccessException {

        MethodHandles.Lookup lookup = context.lookup();
        Class<?> lookupClass = lookup.lookupClass();
        Method method = context.method();
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        Class<?> targetClass = context.targetClass();

        try {
            lookup = MethodHandles.privateLookupIn(targetClass, lookup);
        } catch (IllegalAccessException e) {
            // fallback; LMF will fail but we can fallback to MH invocation
            lookup = lookup.in(targetClass);
        }

        MethodHandle handle;
        try {
            handle = lookup.unreflect(method);
        } catch (IllegalAccessException e) {
            Module lookupModule = lookupClass.getModule();
            Module targetModule = targetClass.getModule();

            if (!lookupModule.canRead(targetModule) || !targetModule.isExported(targetClass.getPackageName(), lookupModule)) {
                throw new ModuleAccessException(e);
            }
            throw new MemberAccessException(e);
        }

        return new UnreflectedContext(lookup, handle, isStatic);
    }

    public record UnreflectedContext(MethodHandles.Lookup lookup, MethodHandle handle, boolean isStatic) {}
}