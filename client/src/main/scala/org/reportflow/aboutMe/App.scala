package org.reportflow.aboutMe
import com.raquo.laminar.api.L.*
import org.scalajs.dom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.Thenable.Implicits.*
import scala.util.Random

@main
def App(): Unit = {

  val pageLayout = div(
    topBarElement(),
    mainContentElement()
  )

  renderOnDomContentLoaded(
    dom.document.getElementById("appContainer"),
    pageLayout
  )
}

def topBarElement(): Element = {
  val contactInfoVisibleVar = Var(false)


  def renderContactInfoDropdown(): Element =
    div(
      cls := "contact-info-dropdown resume-section", // Added a specific class for styling
      // h2(cls := "resume-name", "Eric Smith"), // Name is already in the top bar, maybe omit here or style differently
      p("Greater Seattle Area"),
      p("(509) 906-4820"), // Make sure this is the correct phone number you want to display
      p(a(href := "mailto:data.ericsmith@gmail.com", "data.ericsmith@gmail.com")),
      p(a(href := "https://www.linkedin.com/in/ericdatasmith", target := "_blank", rel := "noopener noreferrer", "Public LinkedIn Profile"))
    )

  div(
    cls := "top-bar",
    span(
      cls := "app-name",
      "Me - Eric Smith" // Your app name or title
    ),
    div(
      cls := "user-icon-container", // This will be our relative positioning anchor
      span(
        cls := "user-icon",
        "👤",
        onClick --> { _ =>
          contactInfoVisibleVar.update(!_) // Toggle the visibility
        }
      ),
      // Conditionally render the contact info box
      child.maybe <-- contactInfoVisibleVar.signal.map { isVisible =>
        if (isVisible) Some(renderContactInfoDropdown()) else None
      }
    )
  )
}

def mainContentElement(): Element = {
  val characterInputVar  = Var("")
  val characterCountVar  = Var(Map.empty[Char, Int])
  val rawDataVar         = Var(Seq.empty[Char])
  val transformedDataVar = Var("")
  val analyzedDataVar   = Var(Map.empty[String, Int])

  val wordsLoadingVar = Var(true)
  val knownWordsVar = Var(Map.empty[Set[Char], Set[String]])

  def loadWordsFromFile(): Unit = {
    wordsLoadingVar.set(true)
    dom.fetch("words_alpha.txt")
      .flatMap(_.text())
      .map{ text =>
        val loadedWords = processWords(text)
        knownWordsVar.set(loadedWords)
        dom.console.log(s"Loaded and processed ${loadedWords.values.map(_.size).sum} words into dictionary")
      }.recover{
        case ex: Throwable =>
          dom.console.error(s"Failed to load or parse words_alpha.txt: ${ex.getMessage}")
      }
      .andThen {
        case _ => wordsLoadingVar.set(false)
      }


  }

  def processWords(wordDoc: String): Map[Set[Char], Set[String]] = wordDoc
    .split("\\s+")
    .map(_.toLowerCase.trim)
    .map(word => (word.toCharArray.toSet, word))
    .groupBy(u => u._1)
    .map { case (charSet, tuples)  =>
      charSet -> tuples.map { case (_, word) =>
        word
      }.toSet
    }

  def handleSubmit(): Unit = {
    val currentInput = characterInputVar.now()
    if (currentInput.length == 1) {
      val char = currentInput.head
      characterCountVar.update {
        currentCounts =>
          val newCount = currentCounts.getOrElse(char.toLower,0) + 1
          currentCounts.updated(char.toLower, newCount)
      }
      characterInputVar.set("")

    } else {
      dom.window.alert("Please enter a single character.")
    }

  }

  def handleDecrement(charToRemove: Char): Unit = {
    characterCountVar.update{currentCounts =>
      currentCounts.get(charToRemove) match {
        case Some(count) if count > 1 =>
          currentCounts.updated(charToRemove, count - 1)
        case Some(_) =>
          currentCounts - charToRemove
        case None =>
          currentCounts
      }
    }
  }

  def findRandomFormableWord(chars: Set[Char], dictionary: Map[Set[Char], Set[String]]): Option[String] = {
    dictionary
      .get(chars)
      .flatMap(stringSet => Random.shuffle(stringSet).headOption)
  }


  val processTickObserver: Observer[Seq[Char]] = Observer { incomingChars =>
    val knownWords = knownWordsVar.now()

    analyzedDataVar.update { currentAnalysis =>

      val maybeWord = transformedDataVar.now() match {
        case x if x.nonEmpty => Some(x)
        case _ => None
      }

      maybeWord match {
        case Some(word) =>
          val newCount = currentAnalysis.getOrElse(word, 0) + 1
          currentAnalysis.updated(word, newCount)
        case None => currentAnalysis
      }
    }



    findRandomFormableWord(incomingChars.toSet, knownWords) match {
      case Some(foundWord) =>
        transformedDataVar.set(foundWord)
      case None =>
        transformedDataVar.set("")
    }
    rawDataVar.set(incomingChars)
  }

  val tickIntervalMs = 1000
  val tickStream: EventStream[Unit] = EventStream.periodic(tickIntervalMs).mapToUnit
  val charsToAddOnTick: EventStream[Seq[Char]] = tickStream.map{ _ =>
    val currentCounts = characterCountVar.now()
    val availableChars = currentCounts.flatMap{
      case (char, count) =>
        Seq.fill(count)(char)
    }
    Random
      .shuffle(availableChars)
      .take(5)
      .toSeq
  }

  // You can place this function outside mainContentElement, for example, after topBarElement
  def renderResumeSection(): Element = {
    div(
      cls := "resume-container", // A class for overall resume styling



      // Summary
      div(
        cls := "resume-summary resume-section",
        h3("Summary"),
        p(
          "Data Engineer with over 10 years of experience, focusing on optimizing data platforms for performance and cost, ",
          "leading complex data infrastructure migrations, building scalable data pipelines and integrations, ",
          "and mentoring and developing engineering talent."
        )
      ),

      // Skills
      div(
        cls := "resume-skills resume-section",
        h3("Skills"),
        ul(
          li(strong("Development - "), "Scala, Python, Docker/Podman, continuous integration and continuous delivery, agile, Jenkins"),
          li(strong("Data - "), "Warehousing (Redshift, Snowflake), Databricks, Spark and Spark structured streaming, Delta Table, RDBMSs (PostgreSQL ), NoSql (DynamoDB), SQL, Kinesis/Kafka, Data modeling, Parquet, statistics"),
          li(strong("AWS - "), "Event Driven Arch. (Serverless), Lambdas, Api Gateway, VPC, RDS, Iam"),
          li(strong("Operations - "), "IaC (Terraform, CDK, Etc. ), observability (New Relic, Data Dog, Cloudwatch)"),
          li(strong("Systems - "), "Ansible, Bash, ZSH, systemd, journald, fedora, debian, Kubernetes")
        )
      ),

      // Experience
      div(
        cls := "resume-experience resume-section",
        h3("Experience"),
        div(
          cls := "resume-job-entry",
          h4("Senior Data Engineer"),
          p(em("The Pokemon Company International, Bellevue, WA"), " | January 2022 - PRESENT"),
          ul(
            li("Led the migration of the e-commerce analytics stack from Snowflake to Databricks, reducing data update latency from 24 hours to 30 minutes, implementing comprehensive data quality checks, and achieving a cost reduction of $60,000 yearly."),
            li("Deployed and managed Airbyte on AWS EKS, replacing existing Fivetran integrations, providing access to hundreds of pre-built data integrations, and reducing annual spend by approximately $100,000."),
            li("Developed custom e-commerce vendor integrations using Airbyte's Python CDK, enabling critical downstream analytics."),
            li("Engineered a type-safe, asynchronous, and concurrent integration application to ingest high-throughput unit test data from qTest."),
            li("Led the Databricks Unity Catalog migration, implementing robust RBAC, data lineage tracking, and team-level sandboxing."),
            li("Implemented a table optimization strategy, reducing data platform costs by 40% and eliminating 400 TB of redundant data.")
          )
        ),
        div(
          cls := "resume-job-entry",
          h4("Data Engineer"),
          p(em("The Pokemon Company International, Bellevue, WA"), " | September 2019 - January 2022"),
          ul(
            li("Founded and led the Data Engineering Team, defining its strategic purpose, service portfolio, and operational standards."),
            li("Engineered and managed a multi-region cloud telemetry ingestion API, ensuring high availability and scalability."),
            li("Refactored the AWS Athena data lake, significantly improving query performance and enabling efficient data analysis."),
            li("Designed and implemented robust CI/CD pipelines using Jenkins, automating software deployments and accelerating release cycles."),
            li("Led the migration to Databricks as a collaborative analytics platform, replacing Athena and enhancing data analysis capabilities."),
            li("Developed a composable trait-based framework for Spark streaming pipelines, significantly simplifying development and deployment, and enabling the delivery of hundreds of streaming tables, resulting in a 80% reduction in code-per-pipeline and reduced time-to-delivery.")
          )
        ),
        div(
          cls := "resume-job-entry",
          h4("Machine Learning Engineer"),
          p(em("Businessolver, Seattle, WA"), " | August 2016 - August 2019"),
          ul(
            li("Project managed development and deployment of AWS data lake"),
            li("Built company-wide data warehouse, delivering analytics from numerous data silos to BI platform"),
            li("Developed in-cloud, machine learning products (Hadoop, Scala, Spark ML)")
          )
        ),
        div(
          cls := "resume-job-entry",
          h4("Data Scientist"),
          p(em("Microsoft (via Team Red Dog), Redmond, WA"), " | August 2016 - August 2019"),
          ul(
            li("Developed and deployed quota distribution system built on machine learning algorithm and optimizing sales quota creation"),
            li("Deployed R programming in 2016 SQL server using Microsoft-R-Open environment"),
            li("Built modeling application to characterize quota characteristics (feature generation)")
          )
        ),
        div(
          cls := "resume-job-entry",
          h4("Post-Doctoral Senior Fellow"),
          p(em("University of Washington, Seattle, WA"), " | August 2016 - August 2019"),
          ul(
            li("Programmed an automated analysis pipeline to process cardiac EM images using Fiji and Java to identify latent cellular pathophysiologies"),
            li("Generated regression models statistical analysis (Linear Reg,T-test, ANOVA) to identify causal relationship between diet and physiology")
          )
        )
      ),

      // Education
      div(
        cls := "resume-education resume-section",
        h3("Education"),
        div(
          cls := "resume-education-entry",
          h4("Doctorate of Philosophy: Oregon State University"),
          p("Biochemistry and Biophysics w/ Focus of Statistics"),
          p("Department of Biochemistry and Biophysics & Linus Pauling Institute"),
          p("August 2008 - June 2014")
        ),
        div(
          cls := "resume-education-entry",
          h4("Baccalaureate of Science: Montana State University"),
          p("Chemistry w/ Biochemistry Option & a Focus of Mathematics"),
          p("Department of Chemistry"),
          p("August 2001 - June 2005")
        )
      ),
      hr(cls := "resume-separator") // Visual separator
    )
  }

  def renderRawDataSpace(title: String, collectionSignal:Signal[Seq[Char]], spaceCls:String): Element =
    div(
      cls := s"collection-space $spaceCls",
      h3(title),
      ul(
        children <-- collectionSignal.map {item =>
          if (item.isEmpty) {
            Seq(li(cls := "empty-collection-placeholder", "Empty"))
          } else {
            item.map(item => li(item.toString))
          }
          }
      )
    )

  def renderTransformedDataSpace(
                                  title: String,
                                  wordSignal: Signal[String],
                                  isLoadedSignal: Signal[Boolean],
                                  spaceCls: String
                                ): Element = {

    def handleSignal(isLoading: Boolean, word: String ) = {
      if (isLoading) {
        Seq(li(cls := "loading-placeholder", "Loading words..."))
      } else {
        if (word.isEmpty) {
          Seq(li(cls := "empty-collection-placeholder", "Empty"))
        } else {
          Seq(li(word))
        }
      }
    }

    val combSignal: Signal[(Boolean,String)] = isLoadedSignal.combineWith(wordSignal)

    div(
      cls := s"collection-space $spaceCls",
      h3(title),
      ul(
        children <-- combSignal.map{case (loading, currentWord) => handleSignal(loading, currentWord)}
      )
    )
  }

  def renderAnalyzedDataSpace(title: String, collectionSignal: Signal[Map[String,Int]], spaceCls: String): Element =
    div(
      cls := s"collection-space $spaceCls",

      h3(title),
      ul(
        children <-- collectionSignal.map { dataMap =>
          if (dataMap.isEmpty) {
            Seq(li(cls := "empty-collection-placeholder", "Empty"))
          } else {
            dataMap
              .toList
              .sortBy{ case (key, _) => key}
              .map{ case (key, value) => li(s"$key: $value") }
          }
        }
      )
    )

  div(
    cls := "main-content",
    onMountCallback(_ => loadWordsFromFile()),
    charsToAddOnTick --> processTickObserver,

    renderResumeSection(),
    h2("Character Counter"),
    div(
      cls:= "input-section",
      input(
        typ := "text",
        placeholder := "Enter one character",
        controlled(
          value <-- characterInputVar,
          onInput.mapToValue --> characterInputVar
        ),
        maxLength := 1
      ),
      button(
        "Submit Character",
        onClick --> (_ => handleSubmit())
      )
    ),

    div(
      cls := "character-count-display",
      h3("Submitted Character Counts:"),
      children <-- characterCountVar.signal.map {
        counts =>
          counts.filter {
            case (_, count) => count > 0
          }
            .toList
            .sortBy{case (char, _) => char }
            .map {
              case (char, count) =>
                div(
                  cls := "count-box",
                  span(s"'$char': $count\t"),
                  button(
                    cls := "remove-one-button",
                    "(-)",
                    onClick --> (_ => handleDecrement(char))
                  )
                )
            }
      }
    ),
    hr(),
    h2("Collection Spaces"),
    div(
      cls := "horizontal-collections-container",
      renderRawDataSpace("Raw Data", rawDataVar.signal, "raw-data-space"),
      renderTransformedDataSpace("Transformed Data", transformedDataVar.signal, wordsLoadingVar.signal, "transformed-data-space"),
      renderAnalyzedDataSpace("Analyzed Data", analyzedDataVar.signal, "analyzed-data-space")
    )



  )
}