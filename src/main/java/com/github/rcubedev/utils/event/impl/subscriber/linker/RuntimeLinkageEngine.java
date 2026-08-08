package com.github.rcubedev.utils.event.impl.subscriber.linker;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.impl.subscriber.BindingFactory;
import com.github.rcubedev.utils.event.impl.subscriber.linker.exception.MemberAccessException;
import com.github.rcubedev.utils.event.impl.subscriber.linker.exception.ModuleAccessException;
import com.github.rcubedev.utils.event.impl.subscriber.linker.exception.StructuralLinkageException;
import com.github.rcubedev.utils.event.impl.subscriber.linker.provider.LmfHandlerLinker;
import com.github.rcubedev.utils.event.impl.subscriber.linker.provider.MethodHandlesHandlerLinker;
import com.github.rcubedev.utils.test.UnitTestIgnored;

public final class RuntimeLinkageEngine implements LinkerEngine {

    private final LinkerEngine primary;
    private final LinkerEngine fallback;

    @UnitTestIgnored
    public RuntimeLinkageEngine() {
        this(new LmfHandlerLinker(), new MethodHandlesHandlerLinker());
    }

    RuntimeLinkageEngine(LinkerEngine primary, LinkerEngine fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public <T extends Event> BindingFactory<T> linkStrong(LinkageContext<T> context) throws StructuralLinkageException, ModuleAccessException, MemberAccessException {
        try {
            return primary.linkStrong(context);
        } catch (ModuleAccessException | MemberAccessException | StructuralLinkageException e) {
            try {
                // todo: add logging
                return fallback.linkStrong(context);
            } catch (ModuleAccessException | MemberAccessException | StructuralLinkageException ex) {
                e.addSuppressed(ex);
                throw e;
            }
        }
    }

    @Override
    public <T extends Event> BindingFactory<T> linkWeak(LinkageContext<T> context) throws StructuralLinkageException, ModuleAccessException, MemberAccessException {
        try {
            return primary.linkWeak(context);
        } catch (ModuleAccessException | MemberAccessException | StructuralLinkageException e) {
            try {
                // todo: add logging
                return fallback.linkWeak(context);
            } catch (ModuleAccessException | MemberAccessException | StructuralLinkageException ex) {
                e.addSuppressed(ex);
                throw e;
            }
        }
    }
}