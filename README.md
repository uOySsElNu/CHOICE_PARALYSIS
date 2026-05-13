# Choice Paralysis / 选择困难症助手

[English](#english) | [中文](#中文)

---

## English

### Overview

Choice Paralysis is an Android app designed for people who struggle with making decisions. Whether you're choosing where to eat, what to watch, or any daily dilemma, this app offers multiple fun and interactive ways to help you decide.

### Features

#### 🎡 Spin Wheel
- Canvas-drawn colorful wheel with smooth rotation animation
- Customize up to 10 options per wheel
- Real-time option editing (add, remove, modify)
- Satisfying deceleration animation with result highlight

#### 🪙 Coin & Dice
- **Coin Flip**: 3D flip animation with heads/tails result
- **Dice Roll**: Bouncing dice animation with 1-6 dot display
- Quick decisions for binary or multiple choices

#### 👍 Yes / No
- Weighted random decisions (Yes 45% / No 45% / Maybe 10%)
- Optional question input for context
- Large emoji-based result display with animations

#### 📋 Custom Lists
- Create and save option lists for repeated use
- Edit list names and options anytime
- Quick access from spin wheel screen
- Persistent storage via DataStore

#### 📜 History
- Automatic recording of all decisions (up to 100 entries)
- Shows decision method, options considered, and result
- Timestamp for each entry
- Clear all or delete individual records

### Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.2.10 |
| UI Framework | Jetpack Compose |
| Design System | Material 3 with Dynamic Color |
| Navigation | Navigation Compose + NavigationSuiteScaffold |
| State Management | ViewModel + StateFlow |
| Persistence | DataStore Preferences + kotlinx-serialization |
| Architecture | MVVM |
| Min SDK | 34 (Android 14) |

### Screenshots

*Coming soon*

### Installation

1. Clone the repository
```bash
git clone https://github.com/yourusername/choice-paralysis.git
```

2. Open in Android Studio

3. Build and run on device or emulator (API 34+)

### Permissions

- None required (all data stored locally)

---

## 中文

### 应用简介

选择困难症助手是一款专为"选择困难症患者"设计的Android应用。无论你是纠结吃什么、看什么，还是面临任何日常抉择，这款应用都能通过多种有趣的方式帮你做出决定。

### 功能特色

#### 🎡 转盘决策
- Canvas绘制的彩色转盘，旋转动画流畅
- 支持自定义最多10个选项
- 实时编辑选项（添加、删除、修改）
- 减速动画效果，结果高亮显示

#### 🪙 硬币与骰子
- **抛硬币**：3D翻转动画，显示正面/反面
- **掷骰子**：弹跳动画，显示1-6点
- 快速决策，适合二选一或多选一场景

#### 👍 Yes / No 决策
- 加权随机决策（是 45% / 否 45% / 也许 10%）
- 可输入问题作为决策背景
- 大号Emoji结果展示，带动画效果

#### 📋 自定义选项列表
- 创建并保存常用选项列表，方便重复使用
- 随时编辑列表名称和选项内容
- 从转盘界面快速加载已保存的列表
- 使用DataStore持久化存储

#### 📜 历史记录
- 自动记录所有决策结果（最多100条）
- 显示决策方式、考虑的选项和最终结果
- 每条记录带时间戳
- 支持清空全部或删除单条记录

### 技术栈

| 组件 | 技术 |
|------|------|
| 开发语言 | Kotlin 2.2.10 |
| UI框架 | Jetpack Compose |
| 设计系统 | Material 3 + 动态取色 |
| 导航 | Navigation Compose + NavigationSuiteScaffold |
| 状态管理 | ViewModel + StateFlow |
| 持久化 | DataStore Preferences + kotlinx-serialization |
| 架构模式 | MVVM |
| 最低SDK | 34 (Android 14) |

### 截图

*即将添加*

### 安装方法

1. 克隆仓库
```bash
git clone https://github.com/yourusername/choice-paralysis.git
```

2. 使用Android Studio打开项目

3. 构建并在设备或模拟器上运行（API 34+）

### 权限说明

- 无需任何权限（所有数据本地存储）

---

## License / 许可证

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

本项目采用 MIT 许可证 - 详情请查看 [LICENSE](LICENSE) 文件。

---

## Contributing / 贡献

Contributions are welcome! Feel free to submit issues and pull requests.

欢迎贡献代码！请随时提交Issue和Pull Request。
