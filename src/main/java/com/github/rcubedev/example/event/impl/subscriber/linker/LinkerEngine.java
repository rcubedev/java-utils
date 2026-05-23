package com.github.rcubedev.example.event.impl.subscriber.linker;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.impl.subscriber.BindingFactory;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.MemberAccessException;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.ModuleAccessException;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.StructuralLinkageException;

/**
 * Strategy for linking method handles to functional interfaces.
 */
public interface LinkerEngine {
    <T extends Event> BindingFactory<T> linkStrong(LinkageContext<T> context) throws StructuralLinkageException, ModuleAccessException, MemberAccessException;
    <T extends Event> BindingFactory<T> linkWeak(LinkageContext<T> context) throws StructuralLinkageException, ModuleAccessException, MemberAccessException;
}