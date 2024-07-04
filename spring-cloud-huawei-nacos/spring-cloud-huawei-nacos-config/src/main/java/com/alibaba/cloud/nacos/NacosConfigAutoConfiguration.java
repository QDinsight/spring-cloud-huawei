/*
 * Copyright 2013-2023 the original author or authors.
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

package com.alibaba.cloud.nacos;

import java.util.List;

import com.huaweicloud.nacos.config.manager.NacosConfigManager;
import com.huaweicloud.nacos.config.manager.NacosConfigServiceMasterManager;
import com.huaweicloud.nacos.config.manager.NacosConfigServiceStandbyManager;
import com.alibaba.cloud.nacos.refresh.NacosContextRefresher;
import com.alibaba.cloud.nacos.refresh.NacosRefreshHistory;
import com.alibaba.cloud.nacos.refresh.SmartConfigurationPropertiesRebinder;
import com.alibaba.cloud.nacos.refresh.condition.ConditionalOnNonDefaultBehavior;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.cloud.context.properties.ConfigurationPropertiesBeans;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Forked and modify from com.alibaba.cloud.nacos.NacosConfigAutoConfiguration.java
 *
 * <p>
 *   add master and standby config service manager bean init
 * </p>
 *
 * @author juven.xuxb
 * @author freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.cloud.nacos.config.enabled", matchIfMissing = true)
public class NacosConfigAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(value = NacosConfigProperties.class, search = SearchStrategy.CURRENT)
  public NacosConfigProperties nacosConfigProperties(ApplicationContext context) {
    if (context.getParent() != null
        && BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
        context.getParent(), NacosConfigProperties.class).length > 0) {
      return BeanFactoryUtils.beanOfTypeIncludingAncestors(context.getParent(),
          NacosConfigProperties.class);
    }
    return new NacosConfigProperties();
  }

  @Bean
  public NacosRefreshHistory nacosRefreshHistory() {
    return new NacosRefreshHistory();
  }

  @Bean
  @ConditionalOnMissingBean
  public NacosConfigServiceMasterManager nacosConfigServiceManagerMaster(
      NacosConfigProperties properties) {
    return new NacosConfigServiceMasterManager(properties);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(value = NacosConfigProperties.PREFIX + ".master-standby-enabled", havingValue = "true")
  public NacosConfigServiceStandbyManager nacosConfigServiceManagerStandby(
      NacosConfigProperties properties) {
    return new NacosConfigServiceStandbyManager(properties);
  }

  @Bean
  public NacosContextRefresher nacosContextRefresher(
      List<NacosConfigManager> nacosConfigManagers, NacosRefreshHistory nacosRefreshHistory,
      NacosConfigProperties properties, Environment env) {
    // Consider that it is not necessary to be compatible with the previous
    // configuration
    // and use the new configuration if necessary.
    return new NacosContextRefresher(nacosConfigManagers, nacosRefreshHistory, properties, env);
  }

  @Bean
  @ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
  @ConditionalOnNonDefaultBehavior
  public ConfigurationPropertiesRebinder smartConfigurationPropertiesRebinder(
      ConfigurationPropertiesBeans beans) {
    // If using default behavior, not use SmartConfigurationPropertiesRebinder.
    // Minimize te possibility of making mistakes.
    return new SmartConfigurationPropertiesRebinder(beans);
  }
}
