package re.vianneyfaiv.persephone.ui.page;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.components.grid.ItemClickListener;

import re.vianneyfaiv.persephone.domain.Application;
import re.vianneyfaiv.persephone.domain.Environment;
import re.vianneyfaiv.persephone.domain.Metrics;
import re.vianneyfaiv.persephone.service.ApplicationService;
import re.vianneyfaiv.persephone.service.EnvironmentService;
import re.vianneyfaiv.persephone.service.MetricsService;
import re.vianneyfaiv.persephone.ui.PersephoneViews;
import re.vianneyfaiv.persephone.ui.fragment.ApplicationOverviewPanel;

/**
 * Page that lists applications.
 *
 * When selecting an application, a details panel will be displayed.
 *
 * {@link ApplicationOverviewPanel}
 */
@UIScope
@SpringView(name=PersephoneViews.APPLICATIONS)
public class ApplicationsPage extends HorizontalLayout implements View {

	@Autowired
	private ApplicationService appService;

	@Autowired
	private EnvironmentService envService;

	@Autowired
	private MetricsService metricsService;

	private Grid<Application> grid;
	private ApplicationOverviewPanel details;

	@PostConstruct
	public void init() {

		this.grid = new Grid<>(Application.class);

		this.grid.removeAllColumns();
		this.grid.addColumn(Application::getName).setCaption("Application");
		this.grid.addColumn(Application::getEnvironment).setCaption("Environment");
		this.grid.addColumn(Application::getUrl).setCaption("URL");

		this.grid.setItems(this.appService.findAll());

		this.grid.setStyleGenerator(app -> {
			return app.isUp() ? null : "app-down";
		});

		this.grid.addItemClickListener(applicationOnClick());

		this.grid.setCaption("<h2>Applications</h2>");
		this.grid.setCaptionAsHtml(true);
		this.grid.setSizeFull();

		this.addComponent(this.grid);
		this.setSizeFull();
	}

	@Override
	public void enter(ViewChangeEvent event) {
	}

	private ItemClickListener<Application> applicationOnClick() {
		return e -> {

			// Remove previous panel
			if(details!=null) {
				this.removeComponent(this.details);
			}

			// Get application overview info
			Application app = e.getItem();
			Environment env = this.envService.getEnvironment(app);
			Metrics metrics = this.metricsService.getMetrics(app);

			// No exception has been thrown : app is up and running !
			app.setUp(true);

			// Add overview panel to page
			this.details = new ApplicationOverviewPanel(app, env, metrics);
			this.addComponent(this.details);
		};
	}
}
