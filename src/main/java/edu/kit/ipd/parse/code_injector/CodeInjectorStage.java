package edu.kit.ipd.parse.code_injector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import edu.kit.ipd.parse.luna.data.PostPipelineData;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.ipd.parse.luna.data.AbstractPipelineData;
import edu.kit.ipd.parse.luna.data.PipelineDataCastException;
import edu.kit.ipd.parse.luna.pipeline.IPipelineStage;
import edu.kit.ipd.parse.luna.pipeline.PipelineStageException;
import edu.kit.ipd.parse.luna.tools.ConfigManager;

/**
 * @author Sebastian Weigelt
 * @author Viktor Kiesel
 */
@MetaInfServices(IPipelineStage.class)
public class CodeInjectorStage implements IPipelineStage {

	private final String ID = "CodeInjectorStage";
	private Properties conf;
	private PostPipelineData appd;

	private File file;

	private String topComment = "Inserted Code:";
	private String header = "here be imports";
	private String classHead = "here be variables";

	private Logger logger = LoggerFactory.getLogger(CodeInjectorStage.class);

	private String COMMENT_SIGN;

	@Override
	public void init() {
		conf = ConfigManager.getConfiguration(CodeInjectorStage.class);
		COMMENT_SIGN = conf.getProperty("CommentSign");
	}

	@Override
	public void exec(AbstractPipelineData data) throws PipelineStageException {
		try {
			appd = data.asPostPipelineData();
			String code = appd.getCode();
			if (appd.isMethod()) {
				injectToAPI(code);
			} else {
				injectToStub(code);
			}

		} catch (PipelineDataCastException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void injectToStub(String code) {

		// Reduce the width of the Comments
		final int TEXT_WIDTH = Integer.valueOf(conf.getProperty("TopCommentWidth"));
		if (TEXT_WIDTH > 0) {
			for (int i = 1; i < (topComment.length() / TEXT_WIDTH) + 1; i++) {
				int space = topComment.indexOf(" ", i * TEXT_WIDTH);
				if (space >= 0) {
					String left = topComment.substring(0, space).trim() + "\n";
					String right = "";
					if (Boolean.valueOf(conf.getProperty("TCCommented"))) {
						right += COMMENT_SIGN;
					}
					right += topComment.substring(space).trim();
					topComment = left + right;
				}
			}
		}

		//Test for proper file 
		JtwigTemplate template = JtwigTemplate.fileTemplate(new File(conf.getProperty("PathToStub")));
		JtwigModel model = JtwigModel.newModel().with("code", "Generated Code:\n" + code).with("topComment", topComment)
				.with("header", header).with("classHead", classHead).with("newMethod", "");

		String runCode = template.render(model);
		if (conf.getProperty("Log").equals("yes")) {
			logger.info("\n" + runCode);
		}

		if (file == null) {
			file = new File(conf.getProperty("ExportTo")); // Use graph-Hash as name?
		}

		file.getParentFile().mkdirs();
		file.delete(); // use counter?

		try {
			file.createNewFile();

			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(runCode);
			bw.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void injectToAPI(String code) {
		JtwigTemplate template = JtwigTemplate.fileTemplate(new File(conf.getProperty("PathToStub")));
		//Add below Keyword 
		String newMethod = "\n" + code;
		newMethod += "\n" + COMMENT_SIGN + " {{ newMethod }}"; //Add Placeholder for new Comment
		JtwigModel model = JtwigModel.newModel().with("newMethod", newMethod);

		String apiCode = template.render(model);

		System.out.println(apiCode);
		if (file == null) {
			file = new File(conf.getProperty("DefaultPathToExtension"));
		}

		file.getParentFile().mkdirs();

		try {
			file.createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(apiCode);
			bw.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public String getID() {
		return ID;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	public void setTopComment(String topComment) {
		this.topComment = topComment;
	}

	public String getTopComment() {
		return topComment;
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public void setClassHead(String classHead) {
		this.classHead = classHead;
	}

	public String getClassHead() {
		return classHead;
	}

}