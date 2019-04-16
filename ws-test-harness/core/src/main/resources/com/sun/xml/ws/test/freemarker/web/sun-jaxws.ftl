<#--

    Copyright (c) 1997, 2019 Oracle and/or its affiliates. All rights reserved.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Distribution License v. 1.0, which is available at
    http://www.eclipse.org/org/documents/edl-v10.php.

    SPDX-License-Identifier: BSD-3-Clause

-->

<endpoints xmlns='http://java.sun.com/xml/ns/jax-ws/ri/runtime' version='2.0'>
<#list endpointInfoBeans as endpoint>
  <endpoint
      name="${endpoint.name}"
      implementation="${endpoint.implementation}"
<#if endpoint.wsdl??>
      wsdl="${endpoint.wsdl}"
</#if>
<#if endpoint.service??>
      service="${endpoint.service}"
</#if>
<#if endpoint.port??>
      port="${endpoint.port}"
</#if>
      url-pattern="${endpoint.urlPattern}">
    <!-- TODO: handlers -->
  </endpoint>
</#list>

</endpoints>
