package com.liferay.portal.security.sso.openid.connect;

public interface OpenIdConnectSession {
	
	public String getAccessTokenString();

	public long getLoginUserId();

	public long getLoginTime();

	public OpenIdConnectFlowState getOpenIdConnectFlowState();

	public void setOpenIdConnectFlowState(OpenIdConnectFlowState openIdConnectFlowState);

}
