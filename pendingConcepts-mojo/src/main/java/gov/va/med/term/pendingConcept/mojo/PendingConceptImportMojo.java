package gov.va.med.term.pendingConcept.mojo;

import gov.va.med.term.pendingConcept.propertyTypes.PT_ContentVersion;
import gov.va.med.term.pendingConcept.propertyTypes.PT_IDs;
import gov.va.oia.terminology.converters.sharedUtils.ConsoleUtil;
import gov.va.oia.terminology.converters.sharedUtils.ConverterBaseMojo;
import gov.va.oia.terminology.converters.sharedUtils.EConceptUtility;
import gov.va.oia.terminology.converters.sharedUtils.EConceptUtility.DescriptionType;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_MemberRefsets;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.PropertyType;
import gov.va.oia.terminology.converters.sharedUtils.stats.ConverterUUID;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.dwfa.cement.ArchitectonicAuxiliary;
import org.ihtsdo.etypes.EConcept;
import org.ihtsdo.tk.dto.concept.component.identifier.TkIdentifier;

/**
 * Goal which converts PendingConcept data into the workbench jbin format
 */
@Mojo (name = "convert-pendingConcept-data", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class PendingConceptImportMojo extends ConverterBaseMojo
{
	private final String pendingConceptNamespaceSeed_ = "gov.va.med.term.pendingConcept";

	private PropertyType ids_;
	private PT_ContentVersion contentVersion_;
	private BPT_MemberRefsets refsets_;

	private EConcept allPendingConceptsRefset;
	
	private HashMap<Long, UUID> existingSctConceptUUIDs_ = new HashMap<>();
	private HashMap<Long, UUID> generatedUUIDs_ = new HashMap<Long, UUID>();
	private HashMap<UUID, String> createdConcepts_ = new HashMap<UUID, String>();

	/**
	 * Location of sct jbin data file. Expected to be a directory.
	 */
	@Parameter (required = true)
	private File sctInputFile;
	
	/**
	 * Source content version number
	 */
	@Parameter (required = true, defaultValue = "${sourceData.version}")
	protected String sourceVersion;
	

	@Override
	public void execute() throws MojoExecutionException
	{
		File outputFolder = outputDirectory;

		try
		{
			if (!outputFolder.exists())
			{
				outputFolder.mkdirs();
			}

			File touch = new File(outputFolder, "PendingConcepts.jbin");
			DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(touch)));

			conceptUtility_ = new EConceptUtility(pendingConceptNamespaceSeed_, "Pending Concepts Path", dos, System.currentTimeMillis());
			
			ids_ = new PT_IDs();

			contentVersion_ = new PT_ContentVersion();
			refsets_ = new BPT_MemberRefsets("Pending Concepts");
			refsets_.addProperty("All Pending Concepts");
			
			
			// Read in the SCT data
			ConsoleUtil.println("Indexing SCT / Reading SCTIDs");
			UUID sctIDType = UUID.fromString("0418a591-f75b-39ad-be2c-3ab849326da9");  //"SNOMED integer id"
			DataInputStream in = new DataInputStream(new FileInputStream(sctInputFile.listFiles(new FilenameFilter()
			{
				@Override
				public boolean accept(File dir, String name)
				{
					if (name.endsWith(".jbin"))
					{
						return true;
					}
					return false;
				}
			})[0]));

			while (in.available() > 0)
			{
				if (existingSctConceptUUIDs_.size() % 1000 == 0)
				{
					ConsoleUtil.showProgress();
				}
				EConcept concept = new EConcept(in);
				
				if (concept.getConceptAttributes() != null && concept.getConceptAttributes().getAdditionalIdComponents() != null)
				{
					for (TkIdentifier id : concept.getConceptAttributes().getAdditionalIdComponents())
					{
						if (sctIDType.equals(id.getAuthorityUuid()))
						{
							//Store these by SCTID, because there is no reliable way to generate a UUID from a SCTID.
							existingSctConceptUUIDs_.put(Long.parseLong(id.getDenotation().toString()), concept.getPrimordialUuid());
							break;
						}
					}
				}
			}
			in.close();
			ConsoleUtil.println("Indexed UUIDs from SCT file - read " + existingSctConceptUUIDs_.size() + " concepts");
			
			ExcelDataReader edr = null;
			for (File f : inputFileLocation.listFiles())
			{
				if (f.isFile() && f.getName().toLowerCase().endsWith(".xlsx"))
				{
					edr = new ExcelDataReader(f);
				}
			}
			
			if (edr == null)
			{
				throw new MojoExecutionException("Failed to find excel file with a .xlsx extension in " + inputFileLocation.getAbsolutePath());
			}
			
			EConcept pendingConceptMetadata = conceptUtility_.createConcept("Pending Concept Metadata", 
					ArchitectonicAuxiliary.Concept.ARCHITECTONIC_ROOT_CONCEPT.getPrimoridalUid());
			
			conceptUtility_.addStringAnnotation(pendingConceptMetadata, sourceVersion, contentVersion_.getProperty("Source Version").getUUID(), false);
			conceptUtility_.addStringAnnotation(pendingConceptMetadata, converterResultVersion, contentVersion_.RELEASE.getUUID(), false);
			conceptUtility_.addStringAnnotation(pendingConceptMetadata, loaderVersion, contentVersion_.LOADER_VERSION.getUUID(), false);
			
			
			pendingConceptMetadata.writeExternal(dos);

			conceptUtility_.loadMetaDataItems(Arrays.asList(ids_, contentVersion_, refsets_), pendingConceptMetadata.getPrimordialUuid(), dos);

			ConsoleUtil.println("Metadata load stats");
			for (String line : conceptUtility_.getLoadStats().getSummary())
			{
				ConsoleUtil.println(line);
			}
			
			conceptUtility_.clearLoadStats();

			allPendingConceptsRefset = refsets_.getConcept("All Pending Concepts");

			ConsoleUtil.println("Reading pending concepts");
			
			ArrayList<PendingConcept> pcs = edr.read();
			
			ConsoleUtil.println("Read " + pcs.size() + " Pending Concepts from the spreadsheet");

			for (PendingConcept pc : pcs)
			{
				writeEConcept(dos, pc);
			}
			
			conceptUtility_.storeRefsetConcepts(refsets_, dos);


			dos.flush();
			dos.close();

			ConsoleUtil.println("Load Statistics");
			// swap out vuids with names to make it more readable...
			for (String line : conceptUtility_.getLoadStats().getSummary())
			{
				ConsoleUtil.println(line);
			}
			
			for (Entry<Long, UUID> x : generatedUUIDs_.entrySet())
			{
				if (!createdConcepts_.containsKey(x.getValue()))
				{
					ConsoleUtil.printErrorln("Missing Parent Pending Concept - " + x.getKey() + " - " + x.getValue());
				}
			}

			// this could be removed from final release. Just added to help debug editor problems.
			ConsoleUtil.println("Dumping UUID Debug File");
			ConverterUUID.dump(outputDirectory, "pendingConceptUuid");

			ConsoleUtil.writeOutputToFile(new File(outputDirectory, "ConsoleOutput.txt").toPath());
		}
		catch (Exception ex)
		{
			throw new MojoExecutionException(ex.getLocalizedMessage(), ex);
		}

	}

	private void writeEConcept(DataOutputStream dos, PendingConcept pendingConcept) throws Exception
	{
		long time = conceptUtility_.defaultTime_;

		UUID newConId = getUUID(pendingConcept.getSCTID());
		
		EConcept concept = conceptUtility_.createConcept(newConId, pendingConcept.getFSN(), time, conceptUtility_.statusCurrentUuid_);
		String temp = createdConcepts_.put(newConId, pendingConcept.getFSN());
		if (temp != null)
		{
			ConsoleUtil.printErrorln("Created the same pending concept twice!  " + pendingConcept.getSCTID() + " - " + pendingConcept.getFSN() + " - " + temp);
		}
		conceptUtility_.addAdditionalIds(concept, pendingConcept.getSCTID() + "", PT_IDs.ID.PENDING_CONCEPT_ID.getProperty().getUUID(), false);

		String preferredDesc = pendingConcept.getFSN();
		if (preferredDesc.endsWith(")") && preferredDesc.contains("("))
		{
			conceptUtility_.addDescription(concept, preferredDesc.substring(0, preferredDesc.lastIndexOf("(") - 1), DescriptionType.SYNONYM, true, null, null, false);
		}
		else
		{
			ConsoleUtil.printErrorln("Pending Concept Description doesn't look like an FSN: '" + preferredDesc + "'");
		}
		
		conceptUtility_.addRelationship(concept, getUUID(pendingConcept.getParentSCTID()), (UUID)null, (Long)null);
		if (pendingConcept.getParentSCTID() != 410662002l)  //'Concept Model Attribute (attribute)' Do not add multiple parents to ROLE concepts, it breaks the classifier.  
		{
			conceptUtility_.addRelationship(concept, allPendingConceptsRefset.getPrimordialUuid(), (UUID)null, (Long)null);
		}
		

		conceptUtility_.addRefsetMember(allPendingConceptsRefset, concept.getPrimordialUuid(), null, true, time);
		concept.writeExternal(dos);
	}
	
	private UUID getUUID(long id)
	{
		if (existingSctConceptUUIDs_.containsKey(id))
		{
			return existingSctConceptUUIDs_.get(id);
		}
		
		if (generatedUUIDs_.containsKey(id))
		{
			return generatedUUIDs_.get(id);
		}
		else
		{
			UUID temp = ConverterUUID.createNamespaceUUIDFromString(id + "");
			generatedUUIDs_.put(id, temp);
			return temp;
		}
	}


	public static void main(String[] args) throws MojoExecutionException
	{
		PendingConceptImportMojo i = new PendingConceptImportMojo();
		i.outputDirectory = new File("../pendingConcepts-econcept/target");
		i.inputFileLocation = new File("../pendingConcepts-econcept/target/generated-resources/");
		i.sctInputFile = new File("../pendingConcepts-econcept/target/generated-resources/SCT");
		i.sourceVersion = "foo";
		i.execute();
	}
}