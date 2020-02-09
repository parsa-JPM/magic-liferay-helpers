package dpco.library.parsa.classes;


import java.io.IOException;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.WindowStateException;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.portlet.LiferayPortletURL;
import com.liferay.portal.kernel.portlet.LiferayWindowState;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.WebKeys;

/**
 * 
 * helper for provide redirects facade
 * 
 * @author p.mihandoost
 * @version 1.0.0
 *
 */
public class Redirect {

	private ActionRequest request;
	private ActionResponse response;



	private Redirect(ActionRequest request, ActionResponse response) {
		this.request = request;
		this.response = response;
	}



	/**
	 * 
	 * redirect to a render command
	 * 
	 * @throws WindowStateException
	 * @throws IOException
	 */
	protected void redirectToRenderCommand(String renderName) throws WindowStateException, IOException {
		LiferayPortletResponse resp = PortalUtil.getLiferayPortletResponse(response);
		LiferayPortletURL pURL = resp.createActionURL("portlet_WAR_name");
		String portletId = (String) request.getAttribute(WebKeys.PORTLET_ID);
		// Setting the action Name
//	    ddUrl.setParameter(ActionRequest.ACTION_NAME, actionName);
		pURL.setParameter("mvcRenderCommandName", renderName);

		pURL.setPortletId(portletId);
		pURL.setWindowState(LiferayWindowState.NORMAL);
		response.sendRedirect(pURL.toString());
	}



	/**
	 * redirect to main page of portlet
	 * 
	 * @throws IOException
	 */
	protected void redirectToMainPage() throws IOException {
		ThemeDisplay themeDisplay =(ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
		String portalURL =  themeDisplay.getPathFriendlyURLPrivateGroup();
		String siteURL =  themeDisplay.getSiteGroup().getFriendlyURL();
		String returnURL = portalURL.concat(siteURL.concat(themeDisplay.getLayout().getFriendlyURL()));
		response.sendRedirect(returnURL);
	}



	/**
	 * 
	 * redirect to a render command shortcut
	 * 
	 * @throws WindowStateException
	 * @throws IOException
	 */
	public static void toRender(String renderName, ActionRequest request, ActionResponse response) {
		Redirect redirect = new Redirect(request, response);

		try {
			redirect.redirectToRenderCommand(renderName);
		} catch (WindowStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	

	/**
	 * redirect to main page of portlet (shortcut)
	 * 
	 * 
	 * @param request
	 * @param response
	 */
	public static void toMain(ActionRequest request, ActionResponse response) {
		Redirect redirect = new Redirect(request, response);
        
		try {
			redirect.redirectToMainPage();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
