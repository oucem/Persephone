package re.vianneyfaiv.persephone.ui;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.annotations.Title;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import re.vianneyfaiv.persephone.domain.Application;
import re.vianneyfaiv.persephone.domain.Environment;
import re.vianneyfaiv.persephone.service.ApplicationService;
import re.vianneyfaiv.persephone.service.EnvironmentService;
import re.vianneyfaiv.persephone.service.PersephoneServiceException;
import re.vianneyfaiv.persephone.ui.page.ApplicationsPage;

@Title("Persephone")
@SpringUI
public class PersephoneUI extends UI {

	@Autowired
	private ApplicationService appService;

	@Autowired
	private EnvironmentService envService;

	@Override
	protected void init(VaadinRequest request) {

		// Get all applications
		List<Application> apps = this.appService.findAll();

		// UI : list applications
		ApplicationsPage appsPage = new ApplicationsPage(apps);

		// Application.onClick => display details
		appsPage.setApplicationClickListener(e -> {
			Environment env = this.envService.getEnvironment(e.getItem());
			appsPage.updateView(env);
		});

		// Build UI
		VerticalLayout layout = new VerticalLayout();
		layout.addComponent(new Label("<h1>Persephone</h1>", ContentMode.HTML));
		layout.addComponent(appsPage);
		this.setContent(layout);

		// TODO : create a class
		UI.getCurrent().setErrorHandler(new DefaultErrorHandler() {
			@Override
		    public void error(com.vaadin.server.ErrorEvent event) {
				for (Throwable t = event.getThrowable(); t != null; t = t.getCause()) {
					if(t instanceof PersephoneServiceException) {

						appsPage.resetDetailsFragment();

						PersephoneServiceException e = (PersephoneServiceException) t;

						new Notification(
								"Unable to reach " + e.getApplication().getUrl(),
							    e.getMessage(),
							    Notification.Type.ERROR_MESSAGE,
							    true)
							.show(Page.getCurrent());
					}
				}
			}
		});
	}
}
