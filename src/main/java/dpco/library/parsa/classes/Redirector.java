package dpco.library.parsa.classes;

import java.io.IOException;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletModeException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletURL;
import javax.portlet.WindowStateException;

import com.liferay.portal.kernel.portlet.LiferayPortletMode;
import com.liferay.portal.kernel.portlet.LiferayWindowState;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;

/**
 * simple redirect to another routes
 * 
 * 
 * @author p.mihandoost
 * @version 1.0.0
 *
 */
public class Redirector {
	private ActionRequest request;
	private ActionResponse response;
	/**
	 * this is portlet name also 
	 */
	private String portletId;

	/**
	 * constructor with action parameters 
	 * 
	 * 
	 * @param request
	 * @param response
	 */
	public Redirector(ActionRequest request, ActionResponse response) {
		this.request = request;
		this.response = response;
		portletId = (String) request.getAttribute(WebKeys.PORTLET_ID);
	}

	/**
	 * redirect to given action 
	 * 
	 * 
	 * @param actionName
	 */
	public void toAction(String actionName) {

		ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);

		PortletURL portletURL = PortletURLFactoryUtil.create(request, portletId, themeDisplay.getPlid(), PortletRequest.ACTION_PHASE);

		try {
			portletURL.setWindowState(LiferayWindowState.NORMAL);
			portletURL.setPortletMode(LiferayPortletMode.VIEW);
		} catch (WindowStateException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (PortletModeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		portletURL.setParameter("javax.portlet.action", actionName);

		request.setAttribute(WebKeys.REDIRECT, portletURL.toString());
		// Here you can redirect to RedirectURL or Portlet URL
		try {
			response.sendRedirect(portletURL.toString());
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

}
