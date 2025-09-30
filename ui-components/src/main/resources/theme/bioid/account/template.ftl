<#import "base/template.ftl" as layout>
<@layout.mainLayout active=active bodyClass=bodyClass; section>
    <#if section = "nav">
        <nav class="navbar navbar-default navbar-pf" role="navigation">
            <div class="navbar-header">
                <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#navbar-collapse-1">
                    <span class="sr-only">Toggle navigation</span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                </button>
                <a href="${url.accountUrl}" class="navbar-brand">
                    <img src="${url.resourcesPath}/img/keycloak-logo-text.png" alt="${kcSanitize(msg("logo"))?no_esc}" />
                </a>
            </div>
            <div class="collapse navbar-collapse" id="navbar-collapse-1">
                <ul class="nav navbar-nav navbar-utility">
                    <li class="dropdown">
                        <a href="#" class="dropdown-toggle" data-toggle="dropdown">
                            <span class="pficon pficon-user"></span>
                            ${msg("fullName")}
                            <b class="caret"></b>
                        </a>
                        <ul class="dropdown-menu">
                            <li><a href="${url.accountUrl}">${msg("account")}</a></li>
                            <li><a href="${url.accountUrl}/password">${msg("password")}</a></li>
                            <li><a href="${url.accountUrl}/totp">${msg("authenticator")}</a></li>
                            <li><a href="${url.accountUrl}/face-credentials">${msg("faceRecognition")}</a></li>
                            <li class="divider"></li>
                            <li><a href="${url.logoutUrl}">${msg("doSignOut")}</a></li>
                        </ul>
                    </li>
                </ul>
            </div>
        </nav>
    <#elseif section = "sidebar">
        <div class="bs-sidebar affix">
            <ul>
                <li class="<#if active=='account'>active</#if>">
                    <a href="${url.accountUrl}">${msg("account")}</a>
                </li>
                <li class="<#if active=='password'>active</#if>">
                    <a href="${url.accountUrl}/password">${msg("password")}</a>
                </li>
                <li class="<#if active=='totp'>active</#if>">
                    <a href="${url.accountUrl}/totp">${msg("authenticator")}</a>
                </li>
                <li class="<#if active=='face-credentials'>active</#if>">
                    <a href="${url.accountUrl}/face-credentials">${msg("faceRecognition")}</a>
                </li>
                <li class="<#if active=='sessions'>active</#if>">
                    <a href="${url.accountUrl}/sessions">${msg("sessions")}</a>
                </li>
                <li class="<#if active=='applications'>active</#if>">
                    <a href="${url.accountUrl}/applications">${msg("applications")}</a>
                </li>
                <#if features.identityFederation>
                    <li class="<#if active=='social'>active</#if>">
                        <a href="${url.accountUrl}/identity">${msg("federatedIdentity")}</a>
                    </li>
                </#if>
                <#if features.log>
                    <li class="<#if active=='log'>active</#if>">
                        <a href="${url.accountUrl}/log">${msg("log")}</a>
                    </li>
                </#if>
                <#if realm.userManagedAccessAllowed && features.authorization>
                    <li class="<#if active=='authorization'>active</#if>">
                        <a href="${url.accountUrl}/resources">${msg("myResources")}</a>
                    </li>
                </#if>
            </ul>
        </div>
    <#else>
        <#nested section>
    </#if>
</@layout.mainLayout>