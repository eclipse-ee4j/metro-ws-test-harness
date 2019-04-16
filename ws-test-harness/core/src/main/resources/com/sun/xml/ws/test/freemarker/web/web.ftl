<#--

    Copyright (c) 1997, 2019 Oracle and/or its affiliates. All rights reserved.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Distribution License v. 1.0, which is available at
    http://www.eclipse.org/org/documents/edl-v10.php.

    SPDX-License-Identifier: BSD-3-Clause

-->

<![CDATA[
<!DOCTYPE web-app PUBLIC "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN" "http://java.sun.com/j2ee/dtds/web-app_2_3.dtd">
]]>
<web-app>
    <display-name>${data.displayName}</display-name>
    <description>${data.description}</description>

    <listener>
        <listener-class>${data.listenerClass}</listener-class>
    </listener>
    <servlet>
        <servlet-name>${data.servletName}</servlet-name>
        <display-name>${data.displayName}</display-name>
        <description>${data.description}</description>
        <servlet-class>${data.servletClass}</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>

    <!-- mappings -->
<#list data.endpoints as endpoint>
    <servlet-mapping>
        <servlet-name>${data.servletName}</servlet-name>
        <url-pattern>${endpoint.urlPattern}</url-pattern>
    </servlet-mapping>
</#list>
    <session-config>
        <session-timeout>60</session-timeout>
    </session-config>
</web-app>
