package com.github.knightliao.canalx.router.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.knightliao.canalx.core.exception.CanalxPluginException;
import com.github.knightliao.canalx.core.exception.CanalxRouterException;
import com.github.knightliao.canalx.core.plugin.IPlugin;
import com.github.knightliao.canalx.core.plugin.router.ICanalxRouter;
import com.github.knightliao.canalx.core.support.annotation.PluginName;
import com.github.knightliao.canalx.core.support.context.ICanalxContext;
import com.github.knightliao.canalx.core.support.context.ICanalxContextAware;
import com.github.knightliao.canalx.core.support.reflection.ReflectionUtil;
import com.github.knightliao.canalx.router.IRouterMgr;

/**
 * @author knightliao
 * @date 2016/12/6 19:49
 */
public class RouterMgrImpl implements IRouterMgr, IPlugin {

    protected static final Logger LOGGER = LoggerFactory.getLogger(RouterMgrImpl.class);

    // injector
    private Map<String, ICanalxRouter> innerCanalxRouterMap = new LinkedHashMap<String, ICanalxRouter>(10);

    //
    private ICanalxRouter firstCanalxRouter = null;

    // context
    private ICanalxContext iCanalxContext = null;

    /**
     * @return
     */
    @Override
    public List<ICanalxRouter> getRouterPlugin() {
        List<ICanalxRouter> iCanalxRouters = new ArrayList<>(10);

        for (String processorName : innerCanalxRouterMap.keySet()) {

            iCanalxRouters.add(innerCanalxRouterMap.get(processorName));
        }

        return iCanalxRouters;
    }

    @Override
    public void runRouter() throws CanalxRouterException {

        if (firstCanalxRouter != null) {

            // run
            firstCanalxRouter.start();
        }
    }

    @Override
    public void init() throws CanalxRouterException {

        if (firstCanalxRouter != null) {

            if (firstCanalxRouter instanceof ICanalxContextAware) {
                ((ICanalxContextAware) firstCanalxRouter).setCanalxContext(iCanalxContext);
            }

            // init
            firstCanalxRouter.init();
        }
    }

    @Override
    public void shutdown() {

        if (firstCanalxRouter != null) {
            try {
                firstCanalxRouter.shutdown();
            } catch (CanalxRouterException e) {
                LOGGER.warn(e.toString());
            }
        }
    }

    @Override
    public void setCanalxContext(ICanalxContext iCanalxContext) {
        this.iCanalxContext = iCanalxContext;
    }

    /**
     * @param scanPack
     * @param specifyPluginNames
     *
     * @throws CanalxPluginException
     */
    @Override
    public void loadPlugin(String scanPack, Set<String> specifyPluginNames) throws CanalxPluginException {

        Reflections reflections = ReflectionUtil.getReflection(scanPack);
        Set<Class<? extends ICanalxRouter>> canalxRouters = reflections.getSubTypesOf(ICanalxRouter
                .class);

        boolean isFirst = true;
        for (Class<? extends ICanalxRouter> canalInjector : canalxRouters) {

            String pluginName = canalInjector.getAnnotation(PluginName.class).name();

            if (!specifyPluginNames.contains(pluginName)) {
                continue;
            }

            LOGGER.info("loading router: {} - {}", pluginName, canalInjector.toString());

            try {
                Class<ICanalxRouter> canalInjectorClass = (Class<ICanalxRouter>) canalInjector;

                innerCanalxRouterMap.put(pluginName, canalInjectorClass.newInstance());

                if (isFirst) {
                    firstCanalxRouter = innerCanalxRouterMap.get(pluginName);
                    isFirst = false;
                }

            } catch (Exception e) {
                LOGGER.error(e.toString());
            }
        }
    }
}
