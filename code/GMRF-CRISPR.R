library("ggplot2", lib.loc="~/R/win-library/4.0")
library("stringr", lib.loc="~/R/win-library/4.0")
setwd("~/Research_GMRF-CRISPR/Results_GMRF-CRISPR_forR")
rm(list = ls())
library(stringr)
library(dplyr,lib.loc="~/R/win-library/4.0")
#library(zoo)

data <- read.csv("GMRF-CRISPRdata_Experiment3.csv")

#attach(data)
#"RANDOM FOREST 1","RANDOM FOREST 2","RANDOM FOREST 3","RANDOM FOREST 4","RANDOM FOREST 5","RANDOM FOREST 6","RANDOM FOREST 7","RANDOM FORET 8","RANDOM FORET 9","RANDOM FORET 10","RANDOM FORET 11","RANDOM FORET 12","RANDOM FORET 13","RANDOM FORET 14","RANDOM FORET 15","RANDOM FORET 16","RANDOM FORET 17","RANDOM FORET 18","RANDOM FORET 19","RANDOM FORET 20",

data$Name <- factor(data$Name, levels=c("RANDOM FOREST 1","RANDOM FOREST 2","RANDOM FOREST 3","RANDOM FOREST 4","RANDOM FOREST 5","RANDOM FOREST 6","RANDOM FOREST 7","RANDOM FORET 8","RANDOM FORET 9","RANDOM FORET 10","RANDOM FORET 11","RANDOM FORET 12","RANDOM FORET 13","RANDOM FORET 14","RANDOM FORET 15","RANDOM FORET 16","RANDOM FORET 17","RANDOM FORET 18","RANDOM FORET 19","RANDOM FORET 20",
                                       "BEST TREES SELECTION MODEL","GENERATION-1","GENERATION-2","GENERATION-3","GENERATION-4","GENERATION-5","GENERATION-6","GENERATION-7","GENERATION-8","GENERATION-9","GENERATION-10","GENERATION-11","GENERATION-12","GENERATION-13","GENERATION-14","GENERATION-15","GENERATION-16","GENERATION-17","GENERATION-18","GENERATION-19","GENERATION-20","GENERATION-21","GENERATION-22","GENERATION-23","GENERATION-24","GENERATION-25","GENERATION-26","GENERATION-27","GENERATION-28","GENERATION-29","GENERATION-30","GENERATION-31","GENERATION-32","GENERATION-33","GENERATION-34","GENERATION-35","GENERATION-36","GENERATION-37","GENERATION-38","GENERATION-39","GENERATION-40","GENERATION-41","GENERATION-42","GENERATION-43","GENERATION-44","GENERATION-45", "GENERATION-56","GENERATION-47","GENERATION-48","GENERATION-49","GENERATION-50"))


data$NumTrees <- factor(data$NumTrees,
                        levels = str_sort(unique(data$NumTrees),
                                          numeric = TRUE),
                        ordered = TRUE)#Know is a factor
data$NumForests <- factor(data$NumForests,
                        levels = str_sort(unique(data$NumForests),
                                          numeric = TRUE),
                        ordered = TRUE)

data$Name <- factor(data$Name, 
                    levels = c("RANDOM FOREST 1","RANDOM FOREST 2","RANDOM FOREST 3","RANDOM FOREST 4","RANDOM FOREST 5","RANDOM FOREST 6","RANDOM FOREST 7","RANDOM FORET 8","RANDOM FORET 9","RANDOM FORET 10","RANDOM FORET 11","RANDOM FORET 12","RANDOM FORET 13","RANDOM FORET 14","RANDOM FORET 15","RANDOM FORET 16","RANDOM FORET 17","RANDOM FORET 18","RANDOM FORET 19","RANDOM FORET 20",
                                          "BEST TREES SELECTION MODEL","GENERATION-1","GENERATION-2","GENERATION-3","GENERATION-4","GENERATION-5","GENERATION-6","GENERATION-7","GENERATION-8","GENERATION-9","GENERATION-10","GENERATION-11","GENERATION-12","GENERATION-13","GENERATION-14","GENERATION-15","GENERATION-16","GENERATION-17","GENERATION-18","GENERATION-19","GENERATION-20","GENERATION-21","GENERATION-22","GENERATION-23","GENERATION-24","GENERATION-25","GENERATION-26","GENERATION-27","GENERATION-28","GENERATION-29","GENERATION-30","GENERATION-31","GENERATION-32","GENERATION-33","GENERATION-34","GENERATION-35","GENERATION-36","GENERATION-37","GENERATION-38","GENERATION-39","GENERATION-40","GENERATION-41","GENERATION-42","GENERATION-43","GENERATION-44","GENERATION-45", "GENERATION-56","GENERATION-47","GENERATION-48","GENERATION-49","GENERATION-50"),
                    labels = c("RF1","RF2","RF3","RF4","RF5","RF6","RF7","RF8","RF9","RF10","RF11","RF12","RF13","RF14","RF15","RF16","RF17","RF18","RF19","RF20",
                               "BTSM","G1","G2","G3","G4","G5","G6","G7","G8","G9","G10","G11","G12","G13","G14","G15","G16","G17","G18","G19","G20","G21","G22","G23","G24","G25","G26","G27","G28","G29","G30","G31","G32","G33","G34","G35","G36","G37","G38","G39","G40","G41","G42","G43","G44","G45","G46","G47","G48","G49","G50"))


#install.packages('zoo')
###########################################################
filter(data, !is.na(Name)) %>% 
  ggplot( 
       aes(Name, Accuracy,
           group = Algo,
           color = str_wrap(Algo,20))) +
  geom_point(size = 4) +
  geom_line(size = 1) +
  scale_shape_manual(values=1:5) +
  ylab("Accuracy") + 
  xlab("Name") +
  theme(text = element_text(size = 10),
        plot.title = element_text(size = 20, hjust = 0.5),
        axis.text.x = element_text(colour="black", angle = 270, hjust = 0, vjust = 0.25),
        panel.grid.major.x = element_blank(),
        panel.grid.major.y = element_line(colour = "black", linetype = "dashed"),
        panel.background = element_rect(fill = NA, color = "black"),
        legend.position = "right",
        legend.key.height=unit(3, "cm"),
        legend.box = "vertical",
        legend.title = element_blank())+
  guides(fill=guide_legend(ncol=2))
###########################################################
filter(data, !is.na(Name)) %>% 
  ggplot(
       aes(x=NumTrees,
           y=Accuracy, 
           group=Name,
           color = Name)) +
  geom_boxplot()+
  facet_wrap(Name ~ .,ncol = 14)
###########################################################
ggplot(data, aes(x = NumForests, y = Accuracy)) +
  geom_boxplot(fill = "grey") +
  facet_grid(. ~ TrainingSplit + NumTrees) +
  scale_x_discrete(name = "Number Forests") +
  scale_y_continuous(name = "Accuracy")+
  theme(text = element_text(size = 20),
        axis.text.x = element_text(colour="black", angle = 90, hjust = 1, vjust = 0.25),
        panel.grid.major.x = element_blank(),
        panel.grid.major.y = element_line(colour = "black", linetype = "dashed"),
        panel.background = element_rect(fill = NA, color = "black"))
###########################################################
data$NumTrees <- factor(data$NumTrees,
                        levels = str_sort(unique(data$NumTrees),
                                          numeric = TRUE),
                        ordered = TRUE)#Know is a factor
data$NumForests <- factor(data$NumForests,
                          levels = str_sort(unique(data$NumForests),
                                            numeric = TRUE),
                          ordered = TRUE)

data$Name <- factor(data$Name, 
                    levels = c("RANDOM FOREST 1","RANDOM FOREST 2","RANDOM FOREST 3","RANDOM FOREST 4","RANDOM FOREST 5","RANDOM FOREST 6","RANDOM FOREST 7","RANDOM FORET 8","RANDOM FORET 9","RANDOM FORET 10","RANDOM FORET 11","RANDOM FORET 12","RANDOM FORET 13","RANDOM FORET 14","RANDOM FORET 15","RANDOM FORET 16","RANDOM FORET 17","RANDOM FORET 18","RANDOM FORET 19","RANDOM FORET 20",
                               "BEST TREES SELECTION MODEL","GENERATION-1","GENERATION-2","GENERATION-3","GENERATION-4","GENERATION-5","GENERATION-6","GENERATION-7","GENERATION-8","GENERATION-9","GENERATION-10","GENERATION-11","GENERATION-12","GENERATION-13","GENERATION-14","GENERATION-15","GENERATION-16","GENERATION-17","GENERATION-18","GENERATION-19","GENERATION-20","GENERATION-21","GENERATION-22","GENERATION-23","GENERATION-24","GENERATION-25","GENERATION-26","GENERATION-27","GENERATION-28","GENERATION-29","GENERATION-30","GENERATION-31","GENERATION-32","GENERATION-33","GENERATION-34","GENERATION-35","GENERATION-36","GENERATION-37","GENERATION-38","GENERATION-39","GENERATION-40","GENERATION-41","GENERATION-42","GENERATION-43","GENERATION-44","GENERATION-45", "GENERATION-56","GENERATION-47","GENERATION-48","GENERATION-49","GENERATION-50"),
                    labels = c("RF1","RF2","RF3","RF4","RF5","RF6","RF7","RF8","RF9","RF10","RF11","RF12","RF13","RF14","RF15","RF16","RF17","RF18","RF19","RF20",
                               "BTSM","G1","G2","G3","G4","G5","G6","G7","G8","G9","G10","G11","G12","G13","G14","G15","G16","G17","G18","G19","G20","G21","G22","G23","G24","G25","G26","G27","G28","G29","G30","G31","G32","G33","G34","G35","G36","G37","G38","G39","G40","G41","G42","G43","G44","G45","G46","G47","G48","G49","G50"))

filter(data, !is.na(Name)) %>% 
  ggplot( aes(x = NumForests, y = Accuracy)) +
    geom_boxplot(fill = "grey") +
    facet_grid(TrainingSplit ~  Name) +
    scale_x_discrete(name = "Number Forests") +
    scale_y_continuous(name = "Accuracy")+
    theme(text = element_text(size = 10),
        axis.text.x = element_text(colour="black", angle = 270, hjust = 1, vjust = 0.25),
        panel.grid.major.x = element_blank(),
        panel.grid.major.y = element_line(colour = "black", linetype = "dashed"),
        panel.background = element_rect(fill = NA, color = "black"))
###########################################################
#NUMFORESTS VS ACCURACY
data <- read.csv("GMRF-CRISPRdata_Experiment3.csv")
data$Name <- factor(data$Name, levels=c("BEST TREES SELECTION MODEL","GENERATION-15"))
data$NumTrees <- factor(data$NumTrees,
                        levels = str_sort(unique(data$NumTrees),
                                          numeric = TRUE),
                        ordered = TRUE)#Know is a factor
data$NumForests <- factor(data$NumForests,
                          levels = str_sort(unique(data$NumForests),
                                            numeric = TRUE),
                          ordered = TRUE)
filter(data, !is.na(Name)) %>% 
  ggplot( aes(x = NumForests, y = Accuracy)) +
  geom_boxplot(fill = "grey") +
  facet_grid(TrainingSplit ~  Name) +
  scale_x_discrete(name = "Number of Forests") +
  scale_y_continuous(name = "Accuracy")+
  theme(text = element_text(size = 10),
        axis.text.x = element_text(colour="black", angle = 270, hjust = 1, vjust = 0.25),
        panel.grid.major.x = element_blank(),
        panel.grid.major.y = element_line(colour = "black", linetype = "dashed"),
        panel.background = element_rect(fill = NA, color = "black"))

###########################################################
#NUMTREES VS ACCURACY
data <- read.csv("GMRF-CRISPRdata_Experiment3.csv")
data$Name <- factor(data$Name, levels=c("BEST TREES SELECTION MODEL","GENERATION-1","GENERATION-2","GENERATION-14","GENERATION-15"))
data$NumTrees <- factor(data$NumTrees,
                        levels = str_sort(unique(data$NumTrees),
                                          numeric = TRUE),
                        ordered = TRUE)#Know is a factor
data$NumForests <- factor(data$NumForests,
                          levels = str_sort(unique(data$NumForests),
                                            numeric = TRUE),
                          ordered = TRUE)
filter(data, !is.na(Name)) %>% 
  ggplot( aes(x = NumTrees, y = Accuracy)) +
  geom_boxplot(fill = "grey") +
  facet_grid(TrainingSplit ~  Name) +
  scale_x_discrete(name = "Number of Trees") +
  scale_y_continuous(name = "Accuracy")+
  theme(text = element_text(size = 10),
        axis.text.x = element_text(colour="black", angle = 270, hjust = 1, vjust = 0.25),
        panel.grid.major.x = element_blank(),
        panel.grid.major.y = element_line(colour = "black", linetype = "dashed"),
        panel.background = element_rect(fill = NA, color = "black"))
  
rlang::last_error()
