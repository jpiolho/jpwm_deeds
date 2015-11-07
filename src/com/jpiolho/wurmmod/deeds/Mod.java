/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jpiolho.wurmmod.deeds;

import com.wurmonline.server.Servers;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.questions.VillageFoundationQuestion;
import com.wurmonline.server.villages.Villages;
import com.wurmonline.server.zones.Zones;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javassist.CtClass;
import javassist.CtPrimitiveType;
import javassist.bytecode.Descriptor;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;

/**
 *
 * @author JPiolho
 */
public class Mod implements WurmMod, Configurable, Initable, ServerStartedListener {

    
    private Integer minimumSize = 5;
    private Long pricePerTile = 100L;
    private Long pricePerPerimeterTile = 50L;
    private Long pricePerGuard = -1L;
    private Long foundingCharge = 30000L;
    
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
   
    @Override
    public void configure(Properties properties) {
        minimumSize = Integer.parseInt(properties.getProperty("minimumSize",Integer.toString(minimumSize)));
        pricePerTile = Long.parseLong(properties.getProperty("pricePerTile",Float.toString(pricePerTile)));
        pricePerPerimeterTile = Long.parseLong(properties.getProperty("pricePerPerimeterTile",Float.toString(pricePerPerimeterTile)));
        pricePerGuard = Long.parseLong(properties.getProperty("pricePerGuard",Float.toString(pricePerGuard)));
        foundingCharge = Long.parseLong(properties.getProperty("foundingCharge",Float.toString(foundingCharge)));
        
        
        logger.log(Level.INFO,"minimumSize: " + minimumSize);
        logger.log(Level.INFO,"pricePerTile: " + pricePerTile);
        logger.log(Level.INFO,"pricePerPerimeterTile: " + pricePerPerimeterTile);
        logger.log(Level.INFO,"pricePerGuard: " + pricePerGuard);
        logger.log(Level.INFO,"foundingCharge: " + foundingCharge);
    }

    @Override
    public void onServerStarted() {
        if(pricePerGuard == -1) {
            pricePerGuard = Villages.GUARD_COST;
        }
    }
    
    
    private long getFoundingCost(VillageFoundationQuestion question) throws NoSuchFieldException, IllegalAccessException
    {
                        
        Class questionClass = question.getClass();
        Field questionTiles = questionClass.getDeclaredField("tiles"); questionTiles.setAccessible(true);
        Field questionPerimeterTiles = questionClass.getDeclaredField("perimeterTiles"); questionPerimeterTiles.setAccessible(true);
        Field questionSelectedGuards = questionClass.getDeclaredField("selectedGuards"); questionSelectedGuards.setAccessible(true);

        long moneyNeeded = (long)questionTiles.getLong(question) * pricePerTile;
        moneyNeeded += (long)questionPerimeterTiles.getLong(question) * pricePerPerimeterTile;
        moneyNeeded += (long)questionSelectedGuards.getLong(question) * pricePerGuard;
        return moneyNeeded;
    }
    
    
    @Override
    public void init() {
        
        
        
        HookManager.getInstance().registerHook("com.wurmonline.server.questions.VillageFoundationQuestion", "getFoundingCost", Descriptor.ofMethod(CtPrimitiveType.longType, new CtClass[] {}), new InvocationHandlerFactory() {

            @Override
            public InvocationHandler createInvocationHandler() {
                return new InvocationHandler() {

                    @Override
                    public Object invoke(Object o, Method method, Object[] os) throws Throwable {
                        try 
                        {
                            return getFoundingCost((VillageFoundationQuestion)o);
                        } 
                        catch(Exception ex)
                        {
                            ex.printStackTrace();
                        }
                        
                        return method.invoke(o, os);
                    }
                };
            }
            
        });
        
        HookManager.getInstance().registerHook("com.wurmonline.server.questions.VillageFoundationQuestion", "getFoundingCharge", Descriptor.ofMethod(CtPrimitiveType.longType, new CtClass[] {}), new InvocationHandlerFactory() {

            @Override
            public InvocationHandler createInvocationHandler() {
                return new InvocationHandler() {

                    @Override
                    public Object invoke(Object o, Method method, Object[] os) throws Throwable {
                        try {
                        VillageFoundationQuestion question = (VillageFoundationQuestion)o;
                        Field questionDeed = question.getClass().getDeclaredField("deed"); questionDeed.setAccessible(true);
                        
                        Item deed = (Item)questionDeed.get(question);
                        return Servers.localServer.isFreeDeeds()?0L:getFoundingCost(question) + foundingCharge - (deed.getTemplateId() == 862?0L:100000L);
                        }
                        catch(Exception ex)
                        {
                            ex.printStackTrace();
                        }
                        
                        return method.invoke(o, os);
                    }
                };
            }
            
        });
        
     
        HookManager.getInstance().registerHook("com.wurmonline.server.questions.VillageFoundationQuestion", "parseVillageFoundationQuestion1", Descriptor.ofMethod(CtPrimitiveType.booleanType, new CtClass[] {}), new InvocationHandlerFactory() {

            @Override
            public InvocationHandler createInvocationHandler() {
                return new InvocationHandler() {

                    @Override
                    public Object invoke(Object object, Method method, Object[] args) throws Throwable {
                        VillageFoundationQuestion question = (VillageFoundationQuestion)object;

                        try {
                        
                            Method questionGetAnswer = ReflectionUtil.getMethod(question.getClass(),"getAnswer");
                            questionGetAnswer.setAccessible(true);

                            Field questionError = question.getClass().getDeclaredField("error");
                            questionError.setAccessible(true);

                            String key = "back";

                            String val = ((Properties)questionGetAnswer.invoke(question)).getProperty(key);
                            
                            if(val != null && val.equals("true")) {
                               Method questionCreateIntro = question.getClass().getDeclaredMethod("createIntro"); questionCreateIntro.setAccessible(true);
                               questionCreateIntro.invoke(question);
                               return false;
                            } else {
                               questionError.set(question, "");
                               question.selectedWest = minimumSize;
                               question.selectedEast = minimumSize;
                               question.selectedNorth = minimumSize;
                               question.selectedSouth = minimumSize;
                               key = "sizeW";
                               val = ((Properties)questionGetAnswer.invoke(question)).getProperty(key);
                               if(val != null) {
                                  try {
                                     question.selectedWest = Integer.parseInt(val);
                                     if(question.selectedWest < minimumSize) {
                                        questionError.set(question,"The minimum size is " + minimumSize + ". ");
                                        question.selectedWest = minimumSize;
                                     }
                                  } catch (NumberFormatException var15) {
                                     questionError.set(question,(String)questionError.get(question) + "* Failed to parse the desired size of " + val + " to a valid number. ");
                                  }
                               }

                               key = "sizeE";
                               val = ((Properties)questionGetAnswer.invoke(question)).getProperty(key);
                               if(val != null) {
                                  try {
                                     question.selectedEast = Integer.parseInt(val);
                                     if(question.selectedEast < minimumSize) {
                                        questionError.set(question,"The minimum size is " + minimumSize + ". ");
                                        question.selectedEast = minimumSize;
                                     }
                                  } catch (NumberFormatException var14) {
                                     questionError.set(question,(String)questionError.get(question) + "* Failed to parse the desired size of " + val + " to a valid number. ");
                                  }
                               }

                               key = "sizeN";
                               val = ((Properties)questionGetAnswer.invoke(question)).getProperty(key);
                               if(val != null) {
                                  try {
                                     question.selectedNorth = Integer.parseInt(val);
                                     if(question.selectedNorth < minimumSize) {
                                        questionError.set(question,"The minimum size is " + minimumSize + ". ");
                                        question.selectedNorth = minimumSize;
                                     }
                                  } catch (NumberFormatException var13) {
                                     questionError.set(question,(String)questionError.get(question) + "Failed to parse the desired size of " + val + " to a valid number. ");
                                  }
                               }

                               key = "sizeS";
                               val = ((Properties)questionGetAnswer.invoke(question)).getProperty(key);
                               if(val != null) {
                                  try {
                                     question.selectedSouth = Integer.parseInt(val);
                                     if(question.selectedSouth < minimumSize) {
                                        questionError.set(question,"The minimum size is " + minimumSize + ". ");
                                        question.selectedSouth = minimumSize;
                                     }
                                  } catch (NumberFormatException var12) {
                                     questionError.set(question,(String)questionError.get(question) + "Failed to parse the desired size of " + val + " to a valid number. ");
                                  }
                               }

                               Field questionDiameterX = question.getClass().getDeclaredField("diameterX"); questionDiameterX.setAccessible(true);
                               Field questionDiameterY = question.getClass().getDeclaredField("diameterY"); questionDiameterY.setAccessible(true);

                               questionDiameterX.set(question,question.selectedWest + question.selectedEast + 1);
                               questionDiameterY.set(question,question.selectedNorth + question.selectedSouth + 1);
                               if((float)((Integer)questionDiameterX.get(question)) / (float)((Integer)questionDiameterY.get(question)) > 4.0F || (float)((Integer)questionDiameterY.get(question)) / (float)((Integer)questionDiameterX.get(question)) > 4.0F) {
                                  questionError.set(question,(String)questionError.get(question) + "The deed would be too stretched. One edge is not allowed to be more than 4 times the length of the other.");
                               }

                               if(((String)questionError.get(question)).length() < 1) {
                                  int xa = Zones.safeTileX(question.tokenx - question.selectedWest);
                                  int xe = Zones.safeTileX(question.tokenx + question.selectedEast);
                                  int ya = Zones.safeTileY(question.tokeny - question.selectedNorth);
                                  int ye = Zones.safeTileY(question.tokeny + question.selectedSouth);

                                  for(int x = xa; x <= xe; ++x) {
                                     for(int y = ya; y <= ye; ++y) {
                                        boolean create = false;
                                        if(x == xa) {
                                           if(y == ya || y == ye || y % 5 == 0) {
                                              create = true;
                                           }
                                        } else if(x == xe) {
                                           if(y == ya || y == ye || y % 5 == 0) {
                                              create = true;
                                           }
                                        } else if((y == ya || y == ye) && x % 5 == 0) {
                                           create = true;
                                        }

                                        if(create) {
                                           try {
                                              Item ex = ItemFactory.createItem(671, 80.0F, question.getResponder().getName());
                                              ex.setPosXYZ((float)((x << 2) + 2), (float)((y << 2) + 2), Zones.calculateHeight((float)((x << 2) + 2), (float)((y << 2) + 2), true) + 5.0F);
                                              Zones.getZone(x, y, true).addItem(ex);
                                           } catch (Exception var11) {
                                              logger.log(Level.INFO, var11.getMessage());
                                           }
                                        }
                                     }
                                  }
                               }

                               Method questionCheckBlockingKingdoms = question.getClass().getDeclaredMethod("checkBlockingKingdoms");
                               questionCheckBlockingKingdoms.setAccessible(true);

                               question.setSize();
                               if(!((Boolean)questionCheckBlockingKingdoms.invoke(question))) {
                                  questionError.set(question,(String)questionError.get(question) + "You would be founding too close to enemy kingdom influence.");
                               }

                               if(((String)questionError.get(question)).length() > 0) {
                                  Method methodquestion1 = question.getClass().getDeclaredMethod("createQuestion1"); methodquestion1.setAccessible(true);
                                  methodquestion1.invoke(question);
                                  return false;
                               } else {
                                  Method methodquestion2 = question.getClass().getDeclaredMethod("createQuestion2"); methodquestion2.setAccessible(true);
                                  methodquestion2.invoke(question);
                                  return true;
                               }

                            }
                        } catch(Exception ex) {
                            ex.printStackTrace();
                        }
                        
                        return method.invoke(object, args);
                    }
                };
            }
        });
    }
    
    
}
